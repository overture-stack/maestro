package bio.overture.maestro.app.infra.adapter.outbound.metadata.study.song;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.StudyDAO;
import bio.overture.masestro.test.TestCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.retry.RetryExhaustedException;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static bio.overture.maestro.app.infra.adapter.outbound.metadata.study.song.SongStudyDAO.STUDY_ID;
import static bio.overture.maestro.domain.utility.Exceptions.wrapWithIndexerException;
import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.text.MessageFormat.format;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@Tag(TestCategory.INT_TEST)
@SpringBootTest(properties = {
    "embedded.elasticsearch.enabled=false"
})
@ContextConfiguration(classes = {SongStudyDAOTest.Config.class})
@AutoConfigureWireMock(port = 0)
class SongStudyDAOTest {

    @Value("${wiremock.server.port}")
    private int wiremockPort;

    @Autowired
    private StudyDAO songStudyDAO;

    @Test
    void shouldRetryOnFailure() {
        val analyses = loadJsonFixture(this.getClass(),
            "PEME-CA.study.json", new TypeReference<List<Analysis>>() {});
        val analysesEither = Either.<IndexerException, List<Analysis>>right(analyses);

        stubFor(
            request("GET", urlEqualTo("/studies/PEME-CA/analysis"))
                .inScenario("RANDOM_FAILURE")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(400)
                    .withBody("<p> some wierd unexpected text </p>")
                    .withHeader("content-type", "text/html")
                )
                .willSetStateTo("WORKING")
        );

        stubFor(
            request("GET", urlEqualTo("/studies/PEME-CA/analysis"))
                .inScenario("RANDOM_FAILURE")
                .whenScenarioStateIs("WORKING")
                .willReturn(ResponseDefinitionBuilder.okForJson(analyses))
        );

        val analysesMono = songStudyDAO.getStudyAnalyses(GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl("http://localhost:"+ wiremockPort)
            .studyId("PEME-CA")
            .build()
        );

        StepVerifier.create(analysesMono)
            .expectNext(analysesEither)
            .verifyComplete();

    }

    @Test
    void shouldReturnErrorIfRetriedExahustedFailure() {
        //given
        val command = GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl("http://localhost:"+ wiremockPort)
            .studyId("PEME-CA")
            .build();
        val expectedException = wrapWithIndexerException(new RetryExhaustedException(),
            format("failed fetching study analysis, command: {0}, retries exhausted",
                command),
            FailureData.builder()
                .failingIds(Map.of(STUDY_ID, Set.of("PEME-CA")))
                .build()
        );
        val expectedResult = Either.<IndexerException, List<Analysis>>left(expectedException);
        stubFor(
            request("GET", urlEqualTo("/studies/PEME-CA/analysis"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withBody("<p> Some wierd unexpected text </p>")
                    .withHeader("content-type", "text/html")
                )
        );

        //when
        val analysesMono = songStudyDAO.getStudyAnalyses(command);

        //then
        StepVerifier.create(analysesMono)
            .expectNextMatches(actual -> actual.isLeft()
                && actual.left().get().getMessage().equals(expectedException.getMessage())
                && actual.left().get().getFailureData().equals(expectedException.getFailureData()))
            .expectComplete()
            .verify();
    }


    @Import({
        SongConfig.class
    })
    @Configuration
    static class Config {
        @Bean
        WebClient webClient() {
            return WebClient.builder().build();
        }

        @Bean
        ApplicationProperties properties() {
            ApplicationProperties properties = mock(ApplicationProperties.class);
            when(properties.songMaxRetries()).thenReturn(3);
            when(properties.songTimeoutSeconds()).thenReturn(20);
            return properties;
        }
    }

}

