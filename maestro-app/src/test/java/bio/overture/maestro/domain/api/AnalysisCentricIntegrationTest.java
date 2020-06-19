package bio.overture.maestro.domain.api;

import bio.overture.maestro.MaestroIntegrationTest;
import bio.overture.maestro.app.infra.adapter.outbound.notification.Slack;
import bio.overture.maestro.app.infra.config.RootConfiguration;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.AnalysisIdentifier;
import bio.overture.maestro.domain.api.message.IndexAnalysisCommand;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDocument;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import bio.overture.masestro.test.TestCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static bio.overture.maestro.domain.api.DefaultIndexer.*;
import static bio.overture.maestro.domain.api.DefaultIndexer.ERR;
import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static bio.overture.masestro.test.Fixture.loadJsonString;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.text.MessageFormat.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@Tag(TestCategory.INT_TEST)
@TestPropertySource(properties = {
  "maestro.elasticsearch.indexes.fileCentric.enabled=false",
  "maestro.elasticsearch.indexes.analysisCentric.enabled=true"
})
public class AnalysisCentricIntegrationTest extends MaestroIntegrationTest {

  @Autowired
  private RestHighLevelClient restHighLevelClient;

  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  private DefaultIndexer indexer;

  @Autowired
  @Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
  private ObjectMapper elasticSearchJsonMapper;

  @SpyBean
  private Notifier notifier;

  @SpyBean
  private Slack slackChannel;

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
    val analyses = loadJsonString(this.getClass(), "TEST-CA.analysis.json");
    val expectedDoc0 =
        loadJsonFixture(
            this.getClass(),
            "analysis-centric-doc0.json",
            AnalysisCentricDocument.class,
            elasticSearchJsonMapper,
            Map.of("COLLAB_REPO_URL", applicationProperties.repositories().get(0).getUrl()));

    stubFor(
        request("GET", urlEqualTo("/collab/studies/TEST-CA/analysis/d0b62734-d955-4b74-b627-34d955eb745e"))
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
                        .analysisId("d0b62734-d955-4b74-b627-34d955eb745e")
                        .studyId("TEST-CA")
                        .repositoryCode("collab")
                        .build())
                .build());

    StepVerifier.create(result)
        .expectNext(IndexResult.builder().indexName(ANALYSIS_CENTRIC_INDEX).successful(true).build())
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
