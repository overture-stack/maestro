package bio.overture.maestro.domain.api;

import static bio.overture.maestro.domain.api.DefaultIndexer.*;
import static bio.overture.maestro.domain.api.DefaultIndexer.ERR;
import static bio.overture.maestro.test.Fixture.loadJsonFixture;
import static bio.overture.maestro.test.Fixture.loadJsonString;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.text.MessageFormat.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import bio.overture.maestro.MaestroIntegrationTest;
import bio.overture.maestro.app.infra.adapter.outbound.notification.Slack;
import bio.overture.maestro.app.infra.config.RootConfiguration;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.*;
import bio.overture.maestro.domain.entities.indexing.Repository;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDocument;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import bio.overture.maestro.test.TestCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
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
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

@Tag(TestCategory.INT_TEST)
@TestPropertySource(
    properties = {
      "maestro.elasticsearch.indexes.fileCentric.enabled=false",
      "maestro.elasticsearch.indexes.analysisCentric.enabled=true"
    })
public class AnalysisCentricIntegrationTest extends MaestroIntegrationTest {

  @Autowired private RestHighLevelClient restHighLevelClient;

  @Autowired private ApplicationProperties applicationProperties;

  @Autowired private DefaultIndexer indexer;

  @Autowired
  @Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
  private ObjectMapper elasticSearchJsonMapper;

  @SpyBean private Notifier notifier;

  @SpyBean private Slack slackChannel;

  private String alias;

  static final String ANALYSIS_CENTRIC_INDEX = "analysis_centric_1.0";

  @BeforeEach
  void setUp() {
    alias = applicationProperties.analysisCentricAlias();
    assertTrue(applicationProperties.isAnalysisCentricIndexEnabled());
    assertFalse(applicationProperties.isFileCentricIndexEnabled());
  }

  @Test
  void shouldIndexAnalysis() throws InterruptedException, IOException {
    // Given
    val analyses = loadJsonString(this.getClass(), "TEST-CA.analysis-1.json");
    val expectedDoc0 =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc4.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

    stubFor(
        request(
                "GET",
                urlEqualTo("/collab/studies/TEST-CA/analysis/f7b2bb0c-f92d-49be-b2bb-0cf92d49be06"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(analyses)
                    .withHeader("Content-Type", "application/json")));

    val result =
        indexer.indexAnalysisToAnalysisCentric(
            IndexAnalysisCommand.builder()
                .analysisIdentifier(
                    AnalysisIdentifier.builder()
                        .analysisId("f7b2bb0c-f92d-49be-b2bb-0cf92d49be06")
                        .studyId("TEST-CA")
                        .repositoryCode("collab")
                        .build())
                .build());

    StepVerifier.create(result)
        .expectNext(
            IndexResult.builder().indexName(ANALYSIS_CENTRIC_INDEX).successful(true).build())
        .verifyComplete();

    Thread.sleep(sleepMillis);

    // assertions
    val docs = getAnalysisCentricDocuments();
    assertNotNull(docs);
    assertEquals(1L, docs.size());
    assertEquals(expectedDoc0, docs.get(0));
  }

  @Test
  void shouldHandleErrorsIfStudyDaoThrowException() {

    val analysisId = "EGAZ00001254368";
    val repoId = "collab";
    val studyId = "PEME-CA";

    // Given
    stubFor(
        request(
                "GET",
                urlEqualTo(format("/{0}/studies/{1}/analysis/{2}", repoId, studyId, analysisId)))
            .willReturn(
                aResponse()
                    .withStatus(500)
                    .withBody("<p> Some weird unexpected text </p>")
                    .withHeader("content-type", "text/html")));

    // checks the notification request
    stubFor(
        request("POST", urlEqualTo("/slack"))
            .withRequestBody(
                equalToJson(
                    "{\"username\":\"maestro\","
                        + "\"text\":\":bangbang: Error : "
                        + NotificationName.FAILED_TO_FETCH_ANALYSIS.name()
                        + ", Error Info: ```{analysisId="
                        + analysisId
                        + ", "
                        + "repoCode="
                        + repoId
                        + ", studyId="
                        + studyId
                        + ", "
                        + "err=org.springframework.web.reactive.function.UnsupportedMediaTypeException: Content type 'text/html' "
                        + "not supported for bodyType=bio.overture.maestro.domain.entities.metadata.studyId.Analysis}```\","
                        + "\"channel\":\"maestro-test\"}"))
            .willReturn(aResponse().withStatus(200).withBody("ok")));

    // test
    val result =
        indexer.indexAnalysisToAnalysisCentric(
            IndexAnalysisCommand.builder()
                .analysisIdentifier(
                    AnalysisIdentifier.builder()
                        .analysisId(analysisId)
                        .studyId(studyId)
                        .repositoryCode(repoId)
                        .build())
                .build());

    StepVerifier.create(result)
        .expectNext(
            IndexResult.builder()
                .failureData(
                    FailureData.builder()
                        .failingIds(Map.of(ANALYSIS_ID, Set.of(analysisId)))
                        .build())
                .successful(false)
                .indexName(ANALYSIS_CENTRIC_INDEX)
                .build())
        .verifyComplete();

    ArgumentMatcher<IndexerNotification> matcher =
        (arg) ->
            arg.getNotificationName().equals(NotificationName.FAILED_TO_FETCH_ANALYSIS)
                && arg.getAttributes().get(ANALYSIS_ID).equals(analysisId)
                && arg.getAttributes().get(REPO_CODE).equals(repoId)
                && arg.getAttributes().get(STUDY_ID).equals(studyId)
                && arg.getAttributes().containsKey(ERR);

    then(notifier).should(times(1)).notify(Mockito.argThat(matcher));
    then(slackChannel).should(times(1)).send(Mockito.argThat(matcher));
  }

  @Test
  void shouldIndexStudyRepositoryWithExclusionsApplied() throws InterruptedException, IOException {
    // study OCCAMS-GB should be excluded.
    // Given
    val studiesArray = loadJsonFixture(this.getClass(), "studies.json", String[].class);
    val studies =
        Arrays.stream(studiesArray)
            .map(s -> Study.builder().studyId(s).build())
            .collect(Collectors.toList());
    val expectedDocs =
        Arrays.asList(
            loadJsonFixture(
                this.getClass(),
                "analysis-centric-docs.json",
                AnalysisCentricDocument[].class,
                elasticSearchJsonMapper,
                Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl())));

    val emptyResp = loadJsonString(this.getClass(), "empty-response.json");
    for (Study study : studies) {
      val studyId = study.getStudyId();
      val resp = getAnalysisResponse(studyId);
      stubFor(
          request(
                  "GET",
                  urlEqualTo(
                      "/collab/studies/"
                          + studyId
                          + "/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=0"))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withBody(resp)
                      .withHeader("Content-Type", "application/json")));
      stubFor(
          request(
                  "GET",
                  urlEqualTo(
                      "/collab/studies/"
                          + studyId
                          + "/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=100"))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withBody(emptyResp)
                      .withHeader("Content-Type", "application/json")));
    }

    stubFor(
        request("GET", urlEqualTo("/collab/studies/all"))
            .willReturn(ResponseDefinitionBuilder.okForJson(studiesArray)));

    // test
    val result =
        indexer.indexRepository(
            IndexStudyRepositoryCommand.builder().repositoryCode("collab").build());

    StepVerifier.create(result)
        .expectNext(
            Map.of(
                ANALYSIS_CENTRIC_INDEX,
                IndexResult.builder().indexName(ANALYSIS_CENTRIC_INDEX).successful(true).build()))
        .verifyComplete();

    Thread.sleep(sleepMillis);

    // assertions
    val docs = getAnalysisCentricDocuments();
    assertNotNull(docs);
    assertEquals(2L, docs.size());
    val sortedExpected =
        expectedDocs.stream()
            .sorted(Comparator.comparing(AnalysisCentricDocument::getAnalysisId))
            .collect(Collectors.toList());
    val sortedDocs =
        docs.stream()
            .sorted(Comparator.comparing(AnalysisCentricDocument::getAnalysisId))
            .collect(Collectors.toList());
    assertEquals(sortedExpected, sortedDocs);
  }

  @Test
  void shouldIndexStudyWithExclusionsApplied() throws InterruptedException, IOException {
    // Given
    @SuppressWarnings("all")
    val analyses = loadJsonString(this.getClass(), "EUCANCAN-BE.response.json");
    val emptyResp = loadJsonString(this.getClass(), "empty-response.json");
    val expectedDoc0 =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc1.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));
    val expectedDoc1 =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc2.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/collab/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=0"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(analyses)
                    .withHeader("Content-Type", "application/json")));
    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/collab/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=100"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(emptyResp)
                    .withHeader("Content-Type", "application/json")));

    // test
    val result =
        indexer.indexStudy(
            IndexStudyCommand.builder().repositoryCode("collab").studyId("EUCANCAN-BE").build());

    StepVerifier.create(result)
        .expectNext(
            IndexResult.builder().indexName(ANALYSIS_CENTRIC_INDEX).successful(true).build())
        .verifyComplete();

    Thread.sleep(sleepMillis);

    // assertions
    val docs = getAnalysisCentricDocuments();

    assertNotNull(docs);
    assertEquals(2L, docs.size());
    assertEquals(
        expectedDoc1,
        docs.stream()
            .filter(d -> d.getAnalysisId().equals(expectedDoc1.getAnalysisId()))
            .findFirst()
            .orElseThrow());
    assertEquals(
        expectedDoc0,
        docs.stream()
            .filter(d -> d.getAnalysisId().equals(expectedDoc0.getAnalysisId()))
            .findFirst()
            .orElseThrow());
  }

  @Test
  void shouldDeleteSingleAnalysis() throws InterruptedException, IOException {
    @SuppressWarnings("all")
    val collabAnalyses = loadJsonString(this.getClass(), "EUCANCAN-BE.studies.json");
    val resp = loadJsonString(this.getClass(), "EUCANCAN-BE.response.json");
    val emptyResp = loadJsonString(this.getClass(), "empty-response.json");
    val expectedDoc0 =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc1.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));
    val expectedDoc1 =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc2.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/collab/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=0"))
            .willReturn(
                aResponse()
                    .withBody(resp)
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)));
    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/collab/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=100"))
            .willReturn(
                aResponse()
                    .withBody(emptyResp)
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)));

    populateIndexWithCollabStudy(expectedDoc0, expectedDoc1, "EUCANCAN-BE");

    // test
    val result =
        indexer.removeAnalysis(
            RemoveAnalysisCommand.builder()
                .analysisIdentifier(
                    AnalysisIdentifier.builder()
                        .analysisId("43f07e4d-e26b-4f4a-b07e-4de26b9f4a50")
                        .studyId("EUCANCAN-BE")
                        .repositoryCode("collab")
                        .build())
                .build());

    StepVerifier.create(result)
        .expectNext(
            IndexResult.builder().indexName(ANALYSIS_CENTRIC_INDEX).successful(true).build())
        .verifyComplete();
    Thread.sleep(sleepMillis);

    // assertions
    val docs = getAnalysisCentricDocuments();
    assertNotNull(docs);
    assertEquals(1L, docs.size());
    assertEquals(expectedDoc0, docs.get(0));
  }

  @Test
  void shouldUpdateExistingDocRepository() throws InterruptedException, IOException {
    // Given
    @SuppressWarnings("all")
    val collabAnalyses = loadJsonString(this.getClass(), "EUCANCAN-BE.response.json");
    @SuppressWarnings("all")
    val awsStudyAnalyses =
        loadJsonString(this.getClass(), "EUCANCAN-BE.aws.analysis.response.json");
    val emptyResp = loadJsonString(this.getClass(), "empty-response.json");

    val expectedDoc0 =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc1.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

    val expectedDoc1 =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc2.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

    val multiRepoDoc =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc3.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of(
                "COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl(),
                "AWS_REPO_URL", applicationProperties.repositories().get(1).getUrl()));
    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/collab/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=0"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(collabAnalyses)
                    .withHeader("Content-Type", "application/json")));
    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/collab/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=100"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(emptyResp)
                    .withHeader("Content-Type", "application/json")));

    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/aws/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=0"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(awsStudyAnalyses)
                    .withHeader("Content-Type", "application/json")));
    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/aws/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=100"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(emptyResp)
                    .withHeader("Content-Type", "application/json")));

    // test
    // step 1
    populateIndexWithCollabStudy(expectedDoc0, expectedDoc1, "EUCANCAN-BE");

    // step 2 index the same analysis from another repository:
    val secondResult =
        indexer.indexStudy(
            IndexStudyCommand.builder().repositoryCode("aws").studyId("EUCANCAN-BE").build());

    StepVerifier.create(secondResult)
        .expectNext(
            IndexResult.builder().indexName(ANALYSIS_CENTRIC_INDEX).successful(true).build())
        .verifyComplete();
    Thread.sleep(sleepMillis);

    // expected results: doc0 should be indexed;
    // analysis 43f07e4d-e26b-4f4a-b07e-4de26b9f4a50 from EUCANCAN-BE exists in both aws and collab
    // repositories,
    // after index, the docs should be merged into one doc with both aws and collab under
    // 'repositories' field.
    val docs = getAnalysisCentricDocuments();
    assertNotNull(docs);
    assertEquals(2L, docs.size());
    assertEquals(
        multiRepoDoc,
        docs.stream()
            .filter(d -> d.getAnalysisId().equals(multiRepoDoc.getAnalysisId()))
            .findFirst()
            .orElseThrow());
    assertEquals(
        expectedDoc0,
        docs.stream()
            .filter(d -> d.getAnalysisId().equals(expectedDoc0.getAnalysisId()))
            .findFirst()
            .orElseThrow());
  }

  @Test
  void shouldDetectAndNotifyConflictingDocuments() throws InterruptedException, IOException {
    // Given
    @SuppressWarnings("all")
    val collabAnalyses = loadJsonString(this.getClass(), "EUCANCAN-BE.studies.json");
    val resp = loadJsonString(this.getClass(), "EUCANCAN-BE.response.json");
    val emptyResp = loadJsonString(this.getClass(), "empty-response.json");

    // Conflicting analysis 43f07e4d-e26b-4f4a-b07e-4de26b9f4a50 has a different analysis state:
    @SuppressWarnings("all")
    val awsStudyAnalyses =
        loadJsonString(this.getClass(), "EUCANCAN-BE.aws.conflicting.study.json");
    val awsAnalysisResp =
        loadJsonString(this.getClass(), "EUCANCAN-BE.aws.conflicting.response.json");
    val awsStudyAnalysesList =
        loadJsonFixture(
            this.getClass(),
            "EUCANCAN-BE.aws.conflicting.study.json",
            new TypeReference<List<Analysis>>() {});
    val expectedDoc0 =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc1.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));
    val expectedDoc1 =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc2.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

    val expectedNotification =
        new IndexerNotification(
            NotificationName.ANALYSIS_CONFLICT,
            getConflicts(expectedDoc1, awsStudyAnalysesList.get(0).getAnalysisId()));

    // stub collab requests:
    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/collab/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=0"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(resp)
                    .withHeader("Content-Type", "application/json")));
    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/collab/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=100"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(emptyResp)
                    .withHeader("Content-Type", "application/json")));
    // stub aws repo requests:
    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/aws/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=0"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(awsAnalysisResp)
                    .withHeader("Content-Type", "application/json")));
    stubFor(
        request(
                "GET",
                urlEqualTo(
                    "/aws/studies/EUCANCAN-BE/analysis/paginated?analysisStates=PUBLISHED&limit=100&offset=100"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(emptyResp)
                    .withHeader("Content-Type", "application/json")));

    // test
    populateIndexWithCollabStudy(expectedDoc0, expectedDoc1, "EUCANCAN-BE");

    // index the same analysis from another repository:
    // test
    val secondResult =
        indexer.indexStudy(
            IndexStudyCommand.builder().repositoryCode("aws").studyId("EUCANCAN-BE").build());

    StepVerifier.create(secondResult)
        .expectNext(
            IndexResult.builder().indexName(ANALYSIS_CENTRIC_INDEX).successful(true).build())
        .verifyComplete();
    Thread.sleep(sleepMillis);

    // assertions
    val docs = getAnalysisCentricDocuments();
    assertNotNull(docs);
    then(notifier).should(times(1)).notify(eq(expectedNotification));
    assertEquals(2L, docs.size());

    assertEquals(
        expectedDoc1,
        docs.stream()
            .filter(d -> d.getAnalysisId().equals(expectedDoc1.getAnalysisId()))
            .findFirst()
            .orElseThrow());
    assertEquals(
        expectedDoc0,
        docs.stream()
            .filter(d -> d.getAnalysisId().equals(expectedDoc0.getAnalysisId()))
            .findFirst()
            .orElseThrow());
  }

  @NotNull
  private Map<String, ? extends Object> getConflicts(
      AnalysisCentricDocument document, String differentAnalysisId) {
    return Map.of(
        "conflicts",
        List.of(
            DefaultIndexer.AnalysisConflict.builder()
                .indexedAnalysis(
                    DefaultIndexer.ConflictingAnalysis.builder()
                        .studyId(document.getStudyId())
                        .analysisId(document.getAnalysisId())
                        .repoCode(
                            document.getRepositories().stream()
                                .map(Repository::getCode)
                                .collect(Collectors.toUnmodifiableSet()))
                        .build())
                .newAnalysis(
                    DefaultIndexer.ConflictingAnalysis.builder()
                        .studyId(document.getStudyId())
                        .analysisId(differentAnalysisId)
                        .repoCode(Set.of("aws"))
                        .build())
                .build()));
  }

  @SneakyThrows
  private void populateIndexWithCollabStudy(
      AnalysisCentricDocument expectedDoc0, AnalysisCentricDocument expectedDoc1, String studyId)
      throws InterruptedException {
    val result =
        indexer.indexStudy(
            IndexStudyCommand.builder().repositoryCode("COLLAB").studyId(studyId).build());
    StepVerifier.create(result)
        .expectNext(
            IndexResult.builder().indexName(ANALYSIS_CENTRIC_INDEX).successful(true).build())
        .verifyComplete();

    Thread.sleep(sleepMillis);

    // assertions
    List<AnalysisCentricDocument> docs = getAnalysisCentricDocuments();

    assertNotNull(docs);
    assertEquals(2L, docs.size());
    assertEquals(
        expectedDoc1,
        docs.stream()
            .filter(d -> d.getAnalysisId().equals(expectedDoc1.getAnalysisId()))
            .findFirst()
            .orElseThrow());
    assertEquals(
        expectedDoc0,
        docs.stream()
            .filter(d -> d.getAnalysisId().equals(expectedDoc0.getAnalysisId()))
            .findFirst()
            .orElseThrow());
  }

  @SneakyThrows
  private String getStudyAnalysesAsString(String studyId) {
    return loadJsonString(getClass(), studyId + ".analysis.json");
  }

  @SneakyThrows
  private String getAnalysisResponse(String studyId) {
    return loadJsonString(getClass(), studyId + ".analysis.response.json");
  }

  @NotNull
  private List<AnalysisCentricDocument> getAnalysisCentricDocuments() throws IOException {
    val searchRequest = new SearchRequest(alias);
    val searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(QueryBuilders.matchAllQuery());
    searchSourceBuilder.from(0);
    searchSourceBuilder.size(100);
    searchRequest.source(searchSourceBuilder);
    val response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
    val docs = new ArrayList<AnalysisCentricDocument>();

    for (SearchHit hit : response.getHits().getHits()) {
      String source = hit.getSourceAsString();
      val doc = elasticSearchJsonMapper.readValue(source, AnalysisCentricDocument.class);
      docs.add(doc);
    }
    return docs;
  }
}
