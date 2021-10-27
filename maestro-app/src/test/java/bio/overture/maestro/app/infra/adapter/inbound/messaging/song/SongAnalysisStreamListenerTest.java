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
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.RemoveAnalysisCommand;
import bio.overture.maestro.domain.entities.message.AnalysisMessage;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.AnalysisTypeId;
import bio.overture.maestro.domain.entities.metadata.study.File;
import bio.overture.maestro.domain.entities.metadata.study.Sample;
import java.text.SimpleDateFormat;
import java.util.List;
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

  private final String ANALYSIS_ID = "050a4af2-87c1-484f-8a4a-f287c1584fcd";
  private final String STUDY_ID = "TEST-CA";
  private final String ANALYSIS_TYPE_NAME = "qc_metrics";
  private final String FILE_OBJECT_ID = "632c1660-9afa-5f6e-9986-4361e373d691";
  private final String FILE_NAME = "test1.tsv";
  private final String FILE_TYPE = "BAM";
  private final String FILE_MD5 = "12345678901234567890123456789012";
  private final String SAMPLE_ID = "SA123";
  private final String SONG_SERVER_ID = "song.collab";

  @Test
  void shouldIndexOnAnalysisPublishedMessage() throws Exception {
    val payload =
        "{\n"
            + "  \"analysisId\": \"050a4af2-87c1-484f-8a4a-f287c1584fcd\",\n"
            + "  \"studyId\": \"TEST-CA\",\n"
            + "  \"state\": \"PUBLISHED\",\n"
            + "  \"action\": \"PUBLISHED\",\n"
            + "  \"songServerId\": \"song.collab\",\n"
            + "  \"analysis\": {\n"
            + "    \"analysisId\": \"050a4af2-87c1-484f-8a4a-f287c1584fcd\",\n"
            + "    \"studyId\": \"TEST-CA\",\n"
            + "    \"analysisState\": \"PUBLISHED\",\n"
            + "    \"updatedAt\": \"2001-07-04T12:08:56.235-0700\",\n"
            + "    \"analysisType\": {\n"
            + "      \"name\": \"qc_metrics\",\n"
            + "      \"version\": 7\n"
            + "    },\n"
            + "    \"samples\": [\n"
            + "      {\n"
            + "        \"sampleId\": \"SA123\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"files\": [\n"
            + "      {\n"
            + "        \"objectId\": \"632c1660-9afa-5f6e-9986-4361e373d691\",\n"
            + "        \"analysisId\": \"050a4af2-87c1-484f-8a4a-f287c1584fcd\",\n"
            + "        \"studyId\": \"TEST-CA\",\n"
            + "        \"fileName\": \"test1.tsv\",\n"
            + "        \"fileType\": \"BAM\",\n"
            + "        \"fileMd5sum\": \"12345678901234567890123456789012\",\n"
            + "        \"fileAccess\": \"open\"\n"
            + "      }\n"
            + "    ]\n"
            + "  }\n"
            + "}\n";
    when(indexer.indexAnalysisFromKafka(any()))
        .thenReturn(Flux.just(IndexResult.builder().successful(true).build()));
    sink.songInput().send(new GenericMessage<>(payload));
    Thread.sleep(2000);

    val df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    val updatedAtString = "2001-07-04T12:08:56.235-0700";
    val updatedAt = df1.parse(updatedAtString);
    then(indexer)
        .should(times(1))
        .indexAnalysisFromKafka(
            eq(
                AnalysisMessage.builder()
                    .analysisId(ANALYSIS_ID)
                    .studyId(STUDY_ID)
                    .state("PUBLISHED")
                    .songServerId(SONG_SERVER_ID)
                    .analysis(
                        Analysis.builder()
                            .analysisId(ANALYSIS_ID)
                            .analysisType(
                                AnalysisTypeId.builder()
                                    .name(ANALYSIS_TYPE_NAME)
                                    .version(7)
                                    .build())
                            .analysisState("PUBLISHED")
                            .updatedAt(updatedAt)
                            .studyId(STUDY_ID)
                            .files(
                                List.of(
                                    File.builder()
                                        .objectId(FILE_OBJECT_ID)
                                        .studyId(STUDY_ID)
                                        .analysisId(ANALYSIS_ID)
                                        .fileName(FILE_NAME)
                                        .fileType(FILE_TYPE)
                                        .fileMd5sum(FILE_MD5)
                                        .fileAccess("open")
                                        .build()))
                            .samples(List.of(Sample.builder().sampleId(SAMPLE_ID).build()))
                            .build())
                    .build()));
  }

  @Test
  void shouldAddOnAnalysisSuppressedMessage() throws Exception {
    val payload =
        "{\n"
            + "  \"analysisId\": \"050a4af2-87c1-484f-8a4a-f287c1584fcd\",\n"
            + "  \"studyId\": \"TEST-CA\",\n"
            + "  \"state\": \"SUPPRESSED\",\n"
            + "  \"action\": \"SUPPRESS\",\n"
            + "  \"songServerId\": \"song.collab\",\n"
            + "  \"analysis\": {\n"
            + "    \"analysisId\": \"050a4af2-87c1-484f-8a4a-f287c1584fcd\",\n"
            + "    \"studyId\": \"TEST-CA\",\n"
            + "    \"analysisState\": \"SUPPRESSED\",\n"
            + "    \"updatedAt\": \"2001-07-04T12:08:56.235-0700\",\n"
            + "    \"analysisType\": {\n"
            + "      \"name\": \"qc_metrics\",\n"
            + "      \"version\": 7\n"
            + "    },\n"
            + "    \"samples\": [\n"
            + "      {\n"
            + "        \"sampleId\": \"SA123\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"files\": [\n"
            + "      {\n"
            + "        \"objectId\": \"632c1660-9afa-5f6e-9986-4361e373d691\",\n"
            + "        \"analysisId\": \"050a4af2-87c1-484f-8a4a-f287c1584fcd\",\n"
            + "        \"studyId\": \"TEST-CA\",\n"
            + "        \"fileName\": \"test1.tsv\",\n"
            + "        \"fileType\": \"BAM\",\n"
            + "        \"fileMd5sum\": \"12345678901234567890123456789012\",\n"
            + "        \"fileAccess\": \"open\"\n"
            + "      }\n"
            + "    ]\n"
            + "  }\n"
            + "}\n";
    when(indexer.indexAnalysisFromKafka(any()))
        .thenReturn(Flux.just(IndexResult.builder().successful(true).build()));
    sink.songInput().send(new GenericMessage<>(payload));
    Thread.sleep(2000);

    val df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    val updatedAtString = "2001-07-04T12:08:56.235-0700";
    val updatedAt = df1.parse(updatedAtString);

    then(indexer)
        .should(times(1))
        .indexAnalysisFromKafka(
            eq(
                AnalysisMessage.builder()
                    .analysisId(ANALYSIS_ID)
                    .studyId(STUDY_ID)
                    .state("SUPPRESSED")
                    .songServerId(SONG_SERVER_ID)
                    .analysis(
                        Analysis.builder()
                            .analysisId(ANALYSIS_ID)
                            .analysisType(
                                AnalysisTypeId.builder()
                                    .name(ANALYSIS_TYPE_NAME)
                                    .version(7)
                                    .build())
                            .analysisState("SUPPRESSED")
                            .updatedAt(updatedAt)
                            .studyId(STUDY_ID)
                            .files(
                                List.of(
                                    File.builder()
                                        .objectId(FILE_OBJECT_ID)
                                        .studyId(STUDY_ID)
                                        .analysisId(ANALYSIS_ID)
                                        .fileName(FILE_NAME)
                                        .fileType(FILE_TYPE)
                                        .fileMd5sum(FILE_MD5)
                                        .fileAccess("open")
                                        .build()))
                            .samples(List.of(Sample.builder().sampleId(SAMPLE_ID).build()))
                            .build())
                    .build()));
  }

  @Test
  void shouldRemoveOnAnalysisUnpublishedMessage() throws Exception {
    val payload =
        "{\n"
            + "  \"analysisId\": \"050a4af2-87c1-484f-8a4a-f287c1584fcd\",\n"
            + "  \"studyId\": \"TEST-CA\",\n"
            + "  \"state\": \"UNPUBLISHED\",\n"
            + "  \"action\": \"UNPUBLISH\",\n"
            + "  \"songServerId\": \"song.collab\",\n"
            + "  \"analysis\": {\n"
            + "    \"analysisId\": \"050a4af2-87c1-484f-8a4a-f287c1584fcd\",\n"
            + "    \"studyId\": \"TEST-CA\",\n"
            + "    \"analysisState\": \"PUBLISHED\",\n"
            + "    \"createdAt\": \"2020-11-30T22:05:45.69915\",\n"
            + "    \"updatedAt\": \"2021-10-27T15:41:29.353913\",\n"
            + "    \"analysisType\": {\n"
            + "      \"name\": \"qc_metrics\",\n"
            + "      \"version\": 7\n"
            + "    },\n"
            + "    \"samples\": [\n"
            + "      {\n"
            + "        \"sampleId\": \"SA123\",\n"
            + "        \"specimenId\": \"SP123\",\n"
            + "        \"submitterSampleId\": \"subSA123\",\n"
            + "        \"matchedNormalSubmitterSampleId\": null,\n"
            + "        \"sampleType\": \"Total DNA\",\n"
            + "        \"donor\": {\n"
            + "          \"donorId\": \"50cae385-92a7-58b0-ab24-59dd1da8c5d6\",\n"
            + "          \"studyId\": \"TEST-CA\",\n"
            + "          \"submitterDonorId\": \"subDO123\",\n"
            + "          \"gender\": \"Male\"\n"
            + "        },\n"
            + "        \"specimen\": {\n"
            + "          \"specimenId\": \"SP123\",\n"
            + "          \"donorId\": \"50cae385-92a7-58b0-ab24-59dd1da8c5d6\",\n"
            + "          \"submitterSpecimenId\": \"subSP123\",\n"
            + "          \"tumourNormalDesignation\": \"Normal\",\n"
            + "          \"specimenTissueSource\": \"Bone\",\n"
            + "          \"specimenType\": \"Normal\"\n"
            + "        }\n"
            + "      }\n"
            + "    ],\n"
            + "    \"files\": [\n"
            + "      {\n"
            + "        \"objectId\": \"632c1660-9afa-5f6e-9986-4361e373d691\",\n"
            + "        \"analysisId\": \"050a4af2-87c1-484f-8a4a-f287c1584fcd\",\n"
            + "        \"studyId\": \"TEST-CA\",\n"
            + "        \"fileName\": \"test1.tsv\",\n"
            + "        \"fileSize\": 123,\n"
            + "        \"fileType\": \"BAM\",\n"
            + "        \"fileMd5sum\": \"12345678901234567890123456789012\",\n"
            + "        \"fileAccess\": \"open\",\n"
            + "        \"dataType\": \"tsv\"\n"
            + "      }\n"
            + "    ]\n"
            + "  }\n"
            + "}\n";
    when(indexer.removeAnalysis(any()))
        .thenReturn(Flux.just(IndexResult.builder().successful(true).build()));
    sink.songInput().send(new GenericMessage<>(payload));
    Thread.sleep(2000);
    then(indexer)
        .should(times(1))
        .removeAnalysis(
            eq(
                RemoveAnalysisCommand.builder()
                    .analysisIdentifier(
                        AnalysisIdentifier.builder()
                            .studyId("TEST-CA")
                            .analysisId("050a4af2-87c1-484f-8a4a-f287c1584fcd")
                            .repositoryCode("song.collab")
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
