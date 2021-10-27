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

import static bio.overture.maestro.app.infra.adapter.inbound.messaging.IndexMessagesHelper.handleIndexResult;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.message.AnalysisIdentifier;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.RemoveAnalysisCommand;
import bio.overture.maestro.domain.entities.message.AnalysisMessage;
import io.vavr.Tuple2;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import reactor.core.publisher.Flux;

@Slf4j
@EnableBinding(SongAnalysisSink.class)
@ConditionalOnExpression(
    "${maestro.disableIndexing} eq false && ${maestro.disableEventIndexing} eq false")
public class SongAnalysisStreamListener {

  private Indexer indexer;
  private final List<String> indexableStudyStatuses;

  public SongAnalysisStreamListener(Indexer indexer, ApplicationProperties properties) {
    this.indexer = indexer;
    this.indexableStudyStatuses = List.of(properties.indexableStudyStatuses().split(","));
  }

  @StreamListener(SongAnalysisSink.NAME)
  public void handleMessage(@Payload AnalysisMessage msg) {
    log.info("received message : {}", msg);
    handleIndexResult(() -> this.doHandle(msg));
  }

  private Flux<Tuple2<AnalysisMessage, IndexResult>> doHandle(AnalysisMessage msg) {
    Flux<IndexResult> result;
    try {
      // only index the analysis when it has a PUBLISHED state.
      if (this.indexableStudyStatuses.contains(msg.getState())) {
        result = indexer.indexAnalysisFromKafka(msg);
      } else {
        val mono =
            indexer.removeAnalysis(
                RemoveAnalysisCommand.builder()
                    .analysisIdentifier(
                        AnalysisIdentifier.builder()
                            .analysisId(msg.getAnalysisId())
                            .studyId(msg.getStudyId())
                            .repositoryCode(msg.getSongServerId())
                            .build())
                    .build());
        result = Flux.from(mono);
      }
      return result.map(indexResult -> new Tuple2<>(msg, indexResult));
    } catch (Exception e) {
      log.error("failed reading message: {} ", msg, e);
      return Flux.just(new Tuple2<>(msg, IndexResult.builder().successful(false).build()));
    }
  }
}
