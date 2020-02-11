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

package bio.overture.maestro.app.infra.adapter.outbound.metadata.study.song;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAnalysisCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.StudyDAO;
import bio.overture.masestro.test.TestCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import lombok.SneakyThrows;
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
import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static bio.overture.masestro.test.Fixture.loadJsonString;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void fetchingAnalysisShouldReturnMonoErrorIfRetriesExhausted() {

        //given
        val analysisId = "EGAZ00001254247";
        val command = GetAnalysisCommand.builder()
            .filesRepositoryBaseUrl("http://localhost:"+ wiremockPort)
            .studyId("PEME-CA")
            .analysisId(analysisId)
            .build();

        stubFor(
            request("GET", urlEqualTo("/studies/PEME-CA/analysis/" + analysisId))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withBody("<p> Some wierd unexpected text </p>")
                    .withHeader("content-type", "text/html")
                )
        );

        //when
        val analysesMono = songStudyDAO.getAnalysis(command);

        //then
        StepVerifier.create(analysesMono)
            .expectErrorSatisfies((e) -> {
                assertTrue(e instanceof RetryExhaustedException);
            }).verify();
    }

    @Test
    @SneakyThrows
    void shouldRetryFetchingAnalysisOnFailure() {
        val analysis = loadJsonString(this.getClass(),
            "PEME-CA.analysis.json");
        val analysisObj = loadJsonFixture(this.getClass(),
            "PEME-CA.analysis.json", Analysis.class);
        val analysisId = "EGAZ00001254247";
        stubFor(
            request("GET", urlEqualTo("/studies/PEME-CA/analysis/" + analysisId))
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
            request("GET", urlEqualTo("/studies/PEME-CA/analysis/" + analysisId))
                .inScenario("RANDOM_FAILURE")
                .whenScenarioStateIs("WORKING")
                .willReturn(aResponse()
                    .withBody(analysis)
                    .withStatus(200)
                    .withHeader("content-type", "application/json")
                )
        );

        val analysesMono = songStudyDAO.getAnalysis(GetAnalysisCommand.builder()
            .filesRepositoryBaseUrl("http://localhost:"+ wiremockPort)
            .studyId("PEME-CA")
            .analysisId(analysisId)
            .build()
        );

        StepVerifier.create(analysesMono)
            .expectNext(analysisObj)
            .verifyComplete();

    }


    @Test
    @SneakyThrows
    void shouldRetryFetchingStudyAnalysesOnFailure() {
        val analyses = loadJsonString(this.getClass(),
            "PEME-CA.studyId.json");
        val analysesList = loadJsonFixture(this.getClass(),
            "PEME-CA.studyId.json",  new TypeReference<List<Analysis>>() {});
        stubFor(
            request("GET", urlEqualTo("/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
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
            request("GET", urlEqualTo("/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
                .inScenario("RANDOM_FAILURE")
                .whenScenarioStateIs("WORKING")
                .willReturn(aResponse()
                    .withBody(analyses)
                    .withStatus(200)
                    .withHeader("content-type", "application/json")
                )
        );

        val analysesMono = songStudyDAO.getStudyAnalyses(GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl("http://localhost:"+ wiremockPort)
            .studyId("PEME-CA")
            .build()
        );

        StepVerifier.create(analysesMono)
            .expectNext(analysesList)
            .verifyComplete();

    }

    @Test
    void fetchingStudyAnalysesShouldReturnRetryExhaustedException() {
        //given
        val command = GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl("http://localhost:"+ wiremockPort)
            .studyId("PEME-CA")
            .build();
        stubFor(
            request("GET", urlEqualTo("/studies/PEME-CA/analysis?analysisStates=PUBLISHED"))
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
            .expectError(RetryExhaustedException.class)
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
            when(properties.songStudyCallTimeoutSeconds()).thenReturn(20);
            when(properties.indexableStudyStatuses()).thenReturn("PUBLISHED");
            return properties;
        }
    }

}

