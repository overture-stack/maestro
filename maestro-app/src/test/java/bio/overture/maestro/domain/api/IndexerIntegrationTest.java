/*
 *  Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package bio.overture.maestro.domain.api;

import bio.overture.maestro.MaestroIntegrationTest;
import bio.overture.maestro.app.infra.adapter.outbound.notification.Slack;
import bio.overture.maestro.app.infra.config.RootConfiguration;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.*;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.entities.indexing.Repository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import bio.overture.masestro.test.TestCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import lombok.SneakyThrows;
import lombok.val;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.api.DefaultIndexer.*;
import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static bio.overture.masestro.test.Fixture.loadJsonString;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.text.MessageFormat.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@Tag(TestCategory.INT_TEST)
class IndexerIntegrationTest extends MaestroIntegrationTest {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    @Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
    private ObjectMapper elasticSearchJsonMapper;

    @SpyBean
    private Notifier notifier;

    @SpyBean
    private Slack slackChannel;

    private String alias;

    @Autowired
    private Indexer indexer;

    @BeforeEach
    void setUp() {
        alias = applicationProperties.fileCentricAlias();
    }

    @Test
    void shouldHandleErrorsIfStudyDaoThrowException() {

        val analysisId = "EGAZ00001254368";
        val repoId = "collab";
        val studyId = "PEME-CA";

        // Given
        stubFor(
            request("GET", urlEqualTo(format("/{0}/studies/{1}/analysis/{2}", repoId, studyId, analysisId)))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("<p> Some weird unexpected text </p>")
                    .withHeader("content-type", "text/html")
                )
        );

        // checks the notification request
        stubFor(request("POST", urlEqualTo("/slack"))
            .withRequestBody(equalToJson("{\"username\":\"maestro\"," +
                "\"text\":\":bangbang: Error : " + NotificationName.FAILED_TO_FETCH_ANALYSIS.name()
                + ", Error Info: ```{analysisId=" + analysisId + ", " +
                "repoCode=" + repoId + ", studyId=" + studyId + ", " +
                "err=org.springframework.web.reactive.function.UnsupportedMediaTypeException: Content type 'text/html' " +
                "not supported for bodyType=bio.overture.maestro.domain.entities.metadata.studyId.Analysis}```\"," +
                "\"channel\":\"maestro-test\"}")
            )
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("ok")
            )
        );

        // test
        val result = indexer.indexAnalysisToFileCentric(IndexAnalysisCommand.builder()
            .analysisIdentifier(AnalysisIdentifier.builder()
                .analysisId(analysisId)
                .studyId(studyId)
                .repositoryCode(repoId)
                .build()
            ).build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder()
                .failureData(
                    FailureData.builder()
                        .failingIds(Map.of(ANALYSIS_ID, Set.of(analysisId))).build()
                ).successful(false).build()
            ).verifyComplete();

        ArgumentMatcher<IndexerNotification> matcher = (arg) ->
             arg.getNotificationName().equals(NotificationName.FAILED_TO_FETCH_ANALYSIS)
                && arg.getAttributes().get(ANALYSIS_ID).equals(analysisId)
                && arg.getAttributes().get(REPO_CODE).equals(repoId)
                && arg.getAttributes().get(STUDY_ID).equals(studyId)
                && arg.getAttributes().containsKey(ERR);

        then(notifier).should(times(1)).notify(Mockito.argThat(matcher));
        then(slackChannel).should(times(1)).send(Mockito.argThat(matcher));

    }

    @Test
    void shouldIndexAnalysis() throws InterruptedException, IOException {
        // Given
        val analyses = loadJsonString(this.getClass(), "PEME-CA.analysis.json");
        val expectedDoc0 = loadJsonFixture(this.getClass(),
            "doc0.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

        stubFor(request("GET", urlEqualTo("/collab/studies/PEME-CA/analysis/EGAZ00001254368"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(analyses)
                .withHeader("Content-Type", "application/json")
            )
        );

        // test
        val result = indexer.indexAnalysisToFileCentric(IndexAnalysisCommand.builder()
            .analysisIdentifier(AnalysisIdentifier.builder()
                .analysisId("EGAZ00001254368")
                .studyId("PEME-CA")
                .repositoryCode("collab")
                .build()
            ).build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder().indexName("file_centric_1.0").successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        val docs = getFileCentricDocuments();

        assertNotNull(docs);
        assertEquals(1L, docs.size());
        assertEquals(expectedDoc0, docs.get(0));
    }

    @Test
    void shouldIndexStudyRepositoryWithExclusionsApplied() throws InterruptedException, IOException {
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
            val studyAnalyses = getStudyAnalysesAsString(studyId);
            stubFor(request("GET", urlEqualTo("/collab/studies/" + studyId + "/analysis?analysisStates=PUBLISHED"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(studyAnalyses)
                        .withHeader("Content-Type", "application/json")
                    )
            );
        }

        stubFor(request("GET", urlEqualTo("/collab/studies/all"))
            .willReturn(ResponseDefinitionBuilder.okForJson(studiesArray)));

        // test
        val result = indexer.indexStudyRepository(IndexStudyRepositoryCommand.builder()
            .repositoryCode("collab")
            .build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder().indexName("file_centric_1.0").successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        val docs = getFileCentricDocuments();
        assertNotNull(docs);
        assertEquals(32L, docs.size());
        val sortedExpected = expectedDocs.stream()
            .sorted(Comparator.comparing(FileCentricDocument::getObjectId))
            .collect(Collectors.toList());
        val sortedDocs = docs.stream()
            .sorted(Comparator.comparing(FileCentricDocument::getObjectId))
            .collect(Collectors.toList());
        assertEquals(sortedExpected, sortedDocs);

    }

    @Test
    void shouldIndexStudyWithExclusionsApplied() throws InterruptedException, IOException {
        // Given
        @SuppressWarnings("all")
        val analyses = loadJsonString(this.getClass(), "PEME-CA.study.json");
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
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(analyses)
                .withHeader("Content-Type", "application/json")
            )
        );

        // test
        val result = indexer.indexStudyToFileCentric(IndexStudyCommand.builder()
            .repositoryCode("collab")
            .studyId("PEME-CA")
            .build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder().indexName("file_centric_1.0").successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        val docs = getFileCentricDocuments();

        assertNotNull(docs);
        assertEquals(2L, docs.size());
        assertEquals(expectedDoc1, docs.get(1));
        assertEquals(expectedDoc0, docs.get(0));
    }

    @Test
    void shouldDeleteSingleAnalysis() throws InterruptedException, IOException {
        // Given
        @SuppressWarnings("all")
        val collabAnalyses = loadJsonString(this.getClass(), "PEME-CA.study.json");
        @SuppressWarnings("all")
        val awsStudyAnalyses = loadJsonString(this.getClass(), "PEME-CA.aws.study.json");
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
            .willReturn(aResponse()
                .withBody(collabAnalyses)
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
            )
        );
        stubFor(request("GET", urlEqualTo("/aws/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
            .willReturn(aResponse()
                .withBody(awsStudyAnalyses)
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
            )
        );

        populateIndexWithCollabStudy(expectedDoc0, expectedDoc1);

        // test
        val result = indexer.removeAnalysis(RemoveAnalysisCommand.builder()
            .analysisIdentifier(
                AnalysisIdentifier.builder()
                    .analysisId("EGAZ00001254247")
                    .studyId("PEME-CA")
                    .repositoryCode("aws")
                    .build()
            ).build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder().successful(true).build())
            .verifyComplete();
        Thread.sleep(sleepMillis);

        // assertions
        val docs = getFileCentricDocuments();
        assertNotNull(docs);
        assertEquals(1L, docs.size());
        assertEquals(expectedDoc0, docs.get(0));

    }

    @Test
    void shouldUpdateExistingFileDocRepository() throws InterruptedException, IOException {
        // Given
        @SuppressWarnings("all")
        val collabAnalyses = loadJsonString(this.getClass(), "PEME-CA.study.json");
        @SuppressWarnings("all")
        val awsStudyAnalyses = loadJsonString(this.getClass(), "PEME-CA.aws.study.json");

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
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(collabAnalyses)
                .withHeader("Content-Type", "application/json")
            )
        );

        stubFor(request("GET", urlEqualTo("/aws/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(awsStudyAnalyses)
                .withHeader("Content-Type", "application/json")
            )
        );

        // test
        // step 1
        populateIndexWithCollabStudy(expectedDoc0, expectedDoc1);

        // step 2 index the same files from another repository:
        val secondResult = indexer.indexStudyToFileCentric(IndexStudyCommand.builder()
            .repositoryCode("aws")
            .studyId("PEME-CA")
            .build());

        StepVerifier.create(secondResult)
            .expectNext(IndexResult.builder().indexName("file_centric_1.0").successful(true).build())
            .verifyComplete();
        Thread.sleep(sleepMillis);

        // assertions
        val docs = getFileCentricDocuments();
        assertNotNull(docs);
        assertEquals(2L, docs.size());
        assertEquals(multiRepoDoc, docs.get(1));
        assertEquals(expectedDoc0, docs.get(0));
    }

    @Test
    void shouldDetectAndNotifyConflictingDocuments() throws InterruptedException, IOException {
        // Given
        @SuppressWarnings("all")
        val collabAnalyses = loadJsonString(this.getClass(), "PEME-CA.study.json");

        // this has a different analysis id than the one in previous files
        @SuppressWarnings("all")
        val awsStudyAnalyses = loadJsonString(this.getClass(), "PEME-CA.aws.conflicting.study.json");
        val awsStudyAnalysesList = loadJsonFixture(this.getClass(), "PEME-CA.aws.conflicting.study.json", new TypeReference<List<Analysis>>() {});
        val expectedDoc0 = loadJsonFixture(this.getClass(),
            "doc0.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));
        val expectedDoc1 = loadJsonFixture(this.getClass(), "doc1.json",
            FileCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

        val expectedNotification = new IndexerNotification(NotificationName.INDEX_FILE_CONFLICT,
            getConflicts(expectedDoc1, awsStudyAnalysesList.get(0).getAnalysisId()));

        stubFor(request("GET", urlEqualTo("/collab/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(collabAnalyses)
                .withHeader("Content-Type", "application/json")
            )
        );
        stubFor(request("GET", urlEqualTo("/aws/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(awsStudyAnalyses)
                .withHeader("Content-Type", "application/json")
            )
        );

        // test
        populateIndexWithCollabStudy(expectedDoc0, expectedDoc1);

        // index the same files from another repository:
        // test
        val secondResult = indexer.indexStudyToFileCentric(IndexStudyCommand.builder()
            .repositoryCode("aws")
            .studyId("PEME-CA")
            .build());

        StepVerifier.create(secondResult)
            .expectNext(IndexResult.builder().indexName("file_centric_1.0").successful(true).build())
            .verifyComplete();
        Thread.sleep(sleepMillis);

        // assertions
        val docs = getFileCentricDocuments();
        assertNotNull(docs);
        then(notifier).should(times(1)).notify(eq(expectedNotification));
        assertEquals(2L, docs.size());
        assertEquals(expectedDoc0, docs.get(0));
        assertEquals(expectedDoc1, docs.get(1));
    }

    @NotNull
    private Map<String, ? extends Object> getConflicts(FileCentricDocument document, String differentAnalysisId) {
        return Map.of("conflicts", List.of(DefaultIndexer.FileConflict.builder()
            .indexedFile(
                DefaultIndexer.ConflictingFile.builder()
                    .studyId(document.getStudyId())
                    .analysisId(document.getAnalysis().getId())
                    .objectId(document.getObjectId())
                    .repoCode(document
                        .getRepositories()
                        .stream().map(Repository::getCode)
                        .collect(Collectors.toUnmodifiableSet()))
                    .build()
            ).newFile(
                DefaultIndexer.ConflictingFile.builder()
                    .studyId(document.getStudyId())
                    .analysisId(differentAnalysisId)
                    .objectId(document.getObjectId())
                    .repoCode(Set.of("aws"))
                    .build()
            ).build()));
    }

    @SneakyThrows
    private void populateIndexWithCollabStudy(FileCentricDocument expectedDoc0, FileCentricDocument expectedDoc1) throws InterruptedException {
        val result = indexer.indexStudyToFileCentric(IndexStudyCommand.builder()
            .repositoryCode("COLLAB")
            .studyId("PEME-CA")
            .build());

        StepVerifier.create(result)
            .expectNext(IndexResult.builder().indexName("file_centric_1.0").successful(true).build())
            .verifyComplete();

        Thread.sleep(sleepMillis);

        // assertions
        List<FileCentricDocument> docs = getFileCentricDocuments();

        assertNotNull(docs);
        assertEquals(2L, docs.size());
        assertEquals(expectedDoc1, docs.get(1));
        assertEquals(expectedDoc0, docs.get(0));
    }

    @NotNull
    private List<FileCentricDocument> getFileCentricDocuments() throws IOException {
        val searchRequest = new SearchRequest(alias);
        val searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(100);
        searchRequest.source(searchSourceBuilder);
        val response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        val docs = new ArrayList<FileCentricDocument>();

        for(SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            val doc = elasticSearchJsonMapper.readValue(source, FileCentricDocument.class);
            docs.add(doc);
        }
        return docs;
    }

    @SneakyThrows
    private String getStudyAnalysesAsString(String studyId) {
        return loadJsonString(getClass(), studyId +".analysis.json");
    }

}