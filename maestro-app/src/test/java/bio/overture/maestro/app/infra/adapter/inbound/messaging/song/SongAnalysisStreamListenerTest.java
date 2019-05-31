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

import bio.overture.maestro.app.infra.adapter.inbound.messaging.MessagingConfig;
import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.message.AnalysisIdentifier;
import bio.overture.maestro.domain.api.message.IndexAnalysisCommand;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;


@Ignore
@SpringBootTest(
    properties = {
        "embedded.kafka.enabled=true",
        "embedded.elasticsearch.enabled=false",
        "embedded.zookeeper.enabled=true",
        "debug=true"
    }
)
@ContextConfiguration(classes = {SongAnalysisStreamListenerTest.Config.class})
class SongAnalysisStreamListenerTest {

    @Autowired
    private Indexer indexer;

    @Value("${embedded.kafka.brokerList}")
    private String kafkaBrokerList;

    @Value("${spring.cloud.stream.bindings.song-input.destination}")
    private String songAnalysisTopic;

    private String analysisPublishedMessage = "{\n" +
        "\t\"records\": [\n" +
        "\t\t{\"value\" : { \"analysisId\" : \"EGAZ00001254368\", \"studyId\" : \"PEME-CA\", \"songServerId\": \"collab\", \"state\": \"PUBLISHED\" }\t}\n" +
        "\t]\n" +
    "}";

    @Test
    void shouldIndexOnAnalysisPublishedMessage() throws Exception {
        sendMessage(songAnalysisTopic, analysisPublishedMessage);
        Thread.sleep(10000);
        then(indexer).should(times(1)).indexAnalysis(eq(IndexAnalysisCommand.builder()
            .analysisIdentifier(AnalysisIdentifier.builder()
                .studyId("PEME-CA")
                .analysisId("EGAZ00001254368")
                .repositoryCode("collab")
                .build())
            .build()
            )
        );
    }

    private Map<String, Object> getKafkaProducerConfiguration() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerList);
        configs.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        configs.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        return configs;
    }

    private void sendMessage(String topic, String message) throws Exception {
        Map<String, Object> producerConfiguration = getKafkaProducerConfiguration();
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(producerConfiguration);
        kafkaProducer.send(new ProducerRecord<>(topic, message)).get();
        kafkaProducer.close();

    }

    @Import({
        MessagingConfig.class
    })
    @Configuration
    static class Config {

        @Bean
        Indexer mockIndexer() {
            return mock(Indexer.class);
        }

    }
}
