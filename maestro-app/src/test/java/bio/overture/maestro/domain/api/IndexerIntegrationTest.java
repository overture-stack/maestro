package bio.overture.maestro.domain.api;

import bio.overture.maestro.MaestroIntegrationTest;
import bio.overture.maestro.app.infra.config.RootConfiguration;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.IndexAnalysisCommand;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import bio.overture.maestro.domain.api.message.IndexStudyRepositoryCommand;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.masestro.test.TestCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import lombok.SneakyThrows;
import lombok.val;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.api.DefaultIndexer.*;
import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(TestCategory.INT_TEST)
@SuppressWarnings("all")
class IndexerIntegrationTest extends MaestroIntegrationTest {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    @Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
    ObjectMapper elasticSearchJsonMapper;

    private String alias;

    @Autowired
    private Indexer indexer;

    @BeforeEach
    void setUp() {
        alias = applicationProperties.fileCentricAlias();
    }

    @Test
    void shouldHandleErrorsIfStudyDaoThrowException() throws InterruptedException {
        // Given
        val analyses = loadJsonFixture(this.getClass(), "PEME-CA.analysis.json", Analysis.class);
        val expectedDoc0 = loadJsonFixture(this.getClass(),
            "doc0.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

        stubFor(
            request("GET", urlEqualTo("/collab/studies/PEME-CA/analysis/EGAZ00001254368"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("<p> Some wierd unexpected text </p>")
                    .withHeader("content-type", "text/html")
                )
        );

        // test
        val result = indexer.indexAnalysis(IndexAnalysisCommand.builder()
            .analysisId("EGAZ00001254368")
            .studyId("PEME-CA")
            .repositoryCode("collab")
            .build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder()
                .failureData(
                    FailureData.builder()
                        .failingIds(
                            Map.of(
                                ANALYSIS_ID, Set.of("EGAZ00001254368"),
                                STUDY_ID, Set.of("PEME-CA"),
                                REPO_CODE, Set.of("collab")
                            )
                        ).build()
                )
                .successful(false).build())
            .verifyComplete();
    }

    @Test
    void shouldIndexAnalysis() throws InterruptedException {
        // Given
        val analyses = loadJsonFixture(this.getClass(), "PEME-CA.analysis.json", Analysis.class);
        val expectedDoc0 = loadJsonFixture(this.getClass(),
            "doc0.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

        stubFor(request("GET", urlEqualTo("/collab/studies/PEME-CA/analysis/EGAZ00001254368"))
            .willReturn(ResponseDefinitionBuilder.okForJson(analyses)));

        // test
        val result = indexer.indexAnalysis(IndexAnalysisCommand.builder()
            .analysisId("EGAZ00001254368")
            .studyId("PEME-CA")
            .repositoryCode("collab")
            .build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder().successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        val query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
            .withIndices(alias)
            .withTypes(alias)
            .build();

        val page = elasticsearchTemplate.queryForPage(query, FileCentricDocument.class, new CustomSearchResultMapper());
        val docs = page.getContent();

        assertNotNull(docs);
        assertEquals(1L, page.getContent().size());
        assertEquals(expectedDoc0, docs.get(0));
    }


    @Test
    void shouldIndexStudyRepositoryWithExclusionsApplied() throws InterruptedException {
        // Given
        val studiesArray = loadJsonFixture(this.getClass(), "studies.json", String[].class);
        val studies = Arrays.stream(studiesArray)
            .map(s-> Study.builder().studyId(s).build())
            .collect(Collectors.toList());
        val expectedDocs = Arrays.asList(
            loadJsonFixture(this.getClass(),
                "docs.json",
                FileCentricDocument[].class, elasticSearchJsonMapper,
                Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl())
            )
        );

        for (Study study: studies) {
            val studyId = study.getStudyId();
            val studyAnalyses = getStudyAnalyses(studyId);
            stubFor(request("GET", urlEqualTo("/collab/studies/" + studyId + "/analysis?analysisStates=PUBLISHED"))
                .willReturn(ResponseDefinitionBuilder.okForJson(studyAnalyses)));
        }

        stubFor(request("GET", urlEqualTo("/collab/studies/all"))
            .willReturn(ResponseDefinitionBuilder.okForJson(studiesArray)));

        // test
        val result = indexer.indexStudyRepository(IndexStudyRepositoryCommand.builder()
            .repositoryCode("collab")
            .build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder().successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        val query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
            .withIndices(alias)
            .withTypes(alias)
            .withPageable(PageRequest.of(0, 100).first())
            .build();

        val page = elasticsearchTemplate.queryForPage(query, FileCentricDocument.class, new CustomSearchResultMapper());
        val docs = page.getContent();

        assertNotNull(docs);
        assertEquals(32L, page.getContent().size());
        val sortedExpected = expectedDocs.stream()
            .sorted(Comparator.comparing(FileCentricDocument::getObjectId))
            .collect(Collectors.toList());
        val sortedDocs = docs.stream()
            .sorted(Comparator.comparing(FileCentricDocument::getObjectId))
            .collect(Collectors.toList());
        assertEquals(sortedExpected, sortedDocs);

    }

    @Test
    void shouldIndexStudyWithExclusionsApplied() throws InterruptedException {
        // Given
        val analyses = loadJsonFixture(this.getClass(), "PEME-CA.study.json", new TypeReference<List<Analysis>>() {});
        val expectedDoc0 = loadJsonFixture(this.getClass(),
            "doc0.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));
        val expectedDoc1 = loadJsonFixture(this.getClass(),
            "doc1.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

        stubFor(request("GET", urlEqualTo("/collab/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
            .willReturn(ResponseDefinitionBuilder.okForJson(analyses)));

        // test
        val result = indexer.indexStudy(IndexStudyCommand.builder()
            .repositoryCode("collab")
            .studyId("PEME-CA")
            .build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder().successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        val query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
            .withIndices(alias)
            .withTypes(alias)
            .build();

        val page = elasticsearchTemplate.queryForPage(query, FileCentricDocument.class, new CustomSearchResultMapper());
        val docs = page.getContent();

        assertNotNull(docs);
        assertEquals(2L, page.getContent().size());
        assertEquals(expectedDoc1, docs.get(1));
        assertEquals(expectedDoc0, docs.get(0));
    }

    @Test
    void shouldUpdateExistingFileDocRepository() throws InterruptedException {
        // Given
        val collabAnalyses = loadJsonFixture(this.getClass(), "PEME-CA.study.json",
            new TypeReference<List<Analysis>>() {});
        val awsStudyAnalyses = loadJsonFixture(this.getClass(), "PEME-CA.aws.study.json",
            new TypeReference<List<Analysis>>() {});

        val expectedDoc0 = loadJsonFixture(this.getClass(),
            "doc0.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

        val expectedDoc1 = loadJsonFixture(this.getClass(), "doc1.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

        val multiRepoDoc = loadJsonFixture(this.getClass(),
            "doc2.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of(
                "COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl(),
                "AWS_REPO_URL", applicationProperties.repositories().get(1).getUrl()
            )
        );

        stubFor(request("GET", urlEqualTo("/collab/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
            .willReturn(ResponseDefinitionBuilder.okForJson(collabAnalyses)));
        stubFor(request("GET", urlEqualTo("/aws/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
            .willReturn(ResponseDefinitionBuilder.okForJson(awsStudyAnalyses)));


        // test
        val result = indexer.indexStudy(IndexStudyCommand.builder()
            .repositoryCode("COLLAB")
            .studyId("PEME-CA")
            .build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder().successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        var query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
            .withIndices(alias)
            .withTypes(alias)
            .build();
        var page = elasticsearchTemplate.queryForPage(query, FileCentricDocument.class, new CustomSearchResultMapper());
        var docs = page.getContent();
        assertNotNull(docs);
        assertEquals(2L, page.getContent().size());
        assertEquals(expectedDoc1, docs.get(1));
        assertEquals(expectedDoc0, docs.get(0));

        // index the same file from another repository:
        // test
        val secondResult = indexer.indexStudy(IndexStudyCommand.builder()
            .repositoryCode("aws")
            .studyId("PEME-CA")
            .build());
        StepVerifier.create(secondResult)
            .expectNext(IndexResult.builder().successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
            .withIndices(alias)
            .withTypes(alias)
            .build();
        page = elasticsearchTemplate.queryForPage(query, FileCentricDocument.class, new CustomSearchResultMapper());
        docs = page.getContent();
        assertNotNull(docs);
        assertEquals(2L, page.getContent().size());
        assertEquals(multiRepoDoc, docs.get(1));
        assertEquals(expectedDoc0, docs.get(0));
    }

    @Test
    void shouldRemoveConflictingDocuments() throws InterruptedException {
        // Given
        val collabAnalyses = loadJsonFixture(this.getClass(), "PEME-CA.study.json",
            new TypeReference<List<Analysis>>() {});
        // this has a different analysis id than the one in previous file
        val awsStudyAnalyses = loadJsonFixture(this.getClass(), "PEME-CA.aws.conflicting.study.json",
            new TypeReference<List<Analysis>>() {});
        val expectedDoc0 = loadJsonFixture(this.getClass(),
            "doc0.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));
        val expectedDoc1 = loadJsonFixture(this.getClass(), "doc1.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

        stubFor(request("GET", urlEqualTo("/collab/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
            .willReturn(ResponseDefinitionBuilder.okForJson(collabAnalyses)));
        stubFor(request("GET", urlEqualTo("/aws/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
            .willReturn(ResponseDefinitionBuilder.okForJson(awsStudyAnalyses)));


        // test
        val result = indexer.indexStudy(IndexStudyCommand.builder()
            .repositoryCode("COLLAB")
            .studyId("PEME-CA")
            .build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder().successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        var query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
            .withIndices(alias)
            .withTypes(alias)
            .build();
        var page = elasticsearchTemplate.queryForPage(query, FileCentricDocument.class, new CustomSearchResultMapper());
        var docs = page.getContent();
        assertNotNull(docs);
        assertEquals(2L, page.getContent().size());
        assertEquals(expectedDoc1, docs.get(1));
        assertEquals(expectedDoc0, docs.get(0));

        // index the same file from another repository:
        // test
        val secondResult = indexer.indexStudy(IndexStudyCommand.builder()
            .repositoryCode("aws")
            .studyId("PEME-CA")
            .build());
        StepVerifier.create(secondResult)
            .expectNext(IndexResult.builder().successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
            .withIndices(alias)
            .withTypes(alias)
            .build();
        page = elasticsearchTemplate.queryForPage(query, FileCentricDocument.class, new CustomSearchResultMapper());
        docs = page.getContent();
        assertNotNull(docs);
        // confirm that now the index only has one document, since the conflicted file was deleted.
        assertEquals(1L, page.getContent().size());
        assertEquals(expectedDoc0, docs.get(0));
    }

    private List<Analysis> getStudyAnalyses(String studyId) {
        return Arrays.asList(loadJsonFixture(getClass(), studyId +".analysis.json", Analysis[].class));
    }

    // this is needed because the json in elastic is snake case
    private class CustomSearchResultMapper implements SearchResultMapper {
        @Override
        @SneakyThrows
        public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
            val docs = new ArrayList<T>();
            for(SearchHit hit : response.getHits().getHits()) {
                String source = hit.getSourceAsString();
                val doc = elasticSearchJsonMapper.readValue(source, clazz);
                docs.add(doc);
            }
            float maxScore = response.getHits().getMaxScore();
            return new AggregatedPageImpl<>(docs, pageable, response.getHits().getTotalHits(),
                response.getAggregations(),
                response.getScrollId(),
                maxScore);
        }

        @Override
        public <T> T mapSearchHit(SearchHit searchHit, Class<T> type) {
            return null;
        }

    }
}