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

package bio.overture.maestro.app.infra.adapter.inbound.messaging.song;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

import bio.overture.maestro.app.infra.adapter.inbound.messaging.MessagingConfig;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.message.AnalysisIdentifier;
import bio.overture.maestro.domain.api.message.IndexAnalysisCommand;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.RemoveAnalysisCommand;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;

/*
 * This test is based on : https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/#_testing
 * Using a real kafka test container didn't turn out as I expected because spring didn't register
 * my consumers (i.e. didn't do the actual binding and connection with the message source).
 * However this test does serve the purpose of testing the listener
 */
@SpringBootTest(classes = {SongAnalysisStreamListenerTest.MockApplication.class})
@ContextConfiguration(classes = {SongAnalysisStreamListenerTest.Config.class})
class SongAnalysisStreamListenerTest {

  @Autowired SongAnalysisSink sink;
  @MockBean private Indexer indexer;

  @Test
  void shouldIndexOnAnalysisPublishedMessage() throws Exception {
    val analysisPublishedMessage =
        "{ \"analysisId\" : \"EGAZ00001254368\", \"studyId\" : \"PEME-CA\", "
            + "\"songServerId\": \"collab\", \"state\": \"PUBLISHED\" }";
    when(indexer.indexAnalysis(any()))
        .thenReturn(Flux.just(IndexResult.builder().successful(true).build()));
    sink.songInput().send(new GenericMessage<>(analysisPublishedMessage));
    Thread.sleep(2000);
    then(indexer)
        .should(times(1))
        .indexAnalysis(
            eq(
                IndexAnalysisCommand.builder()
                    .analysisIdentifier(
                        AnalysisIdentifier.builder()
                            .studyId("PEME-CA")
                            .analysisId("EGAZ00001254368")
                            .repositoryCode("collab")
                            .build())
                    .build()));
  }

  @Test
  void shouldAddOnAnalysisSuppressedMessage() throws Exception {
    val analysisSuppressedMessage =
        "{ \"analysisId\" : \"EGAZ00001254368\", \"studyId\" : \"PEME-CA\", "
            + "\"songServerId\": \"collab\", \"state\": \"SUPPRESSED\" }";
    when(indexer.indexAnalysis(any()))
        .thenReturn(Flux.just(IndexResult.builder().successful(true).build()));
    sink.songInput().send(new GenericMessage<>(analysisSuppressedMessage));
    Thread.sleep(2000);
    then(indexer)
        .should(times(1))
        .indexAnalysis(
            eq(
                IndexAnalysisCommand.builder()
                    .analysisIdentifier(
                        AnalysisIdentifier.builder()
                            .studyId("PEME-CA")
                            .analysisId("EGAZ00001254368")
                            .repositoryCode("collab")
                            .build())
                    .build()));
  }

  @Test
  void shouldRemoveOnAnalysisUnpublishedMessage() throws Exception {
    val analysisPublishedMessage =
        "{ \"analysisId\" : \"EGAZ00001254368\", \"studyId\" : \"PEME-CA\", "
            + "\"songServerId\": \"collab\", \"state\": \"UNPUBLISHED\" }";
    when(indexer.removeAnalysis(any()))
        .thenReturn(Flux.just(IndexResult.builder().successful(true).build()));
    sink.songInput().send(new GenericMessage<>(analysisPublishedMessage));
    Thread.sleep(2000);
    then(indexer)
        .should(times(1))
        .removeAnalysis(
            eq(
                RemoveAnalysisCommand.builder()
                    .analysisIdentifier(
                        AnalysisIdentifier.builder()
                            .studyId("PEME-CA")
                            .analysisId("EGAZ00001254368")
                            .repositoryCode("collab")
                            .build())
                    .build()));
  }

  /*
   * This is needed or you will get :
   * org.springframework.integration.MessageDispatchingException: Dispatcher has no subscribers
   */
  @SpringBootApplication
  @Import({MessagingConfig.class})
  static class MockApplication {}

  @Configuration
  static class Config {
    @Bean
    ApplicationProperties properties() {
      ApplicationProperties properties = mock(ApplicationProperties.class);
      when(properties.indexableStudyStatuses()).thenReturn("PUBLISHED,SUPPRESSED");
      return properties;
    }
  }
}
