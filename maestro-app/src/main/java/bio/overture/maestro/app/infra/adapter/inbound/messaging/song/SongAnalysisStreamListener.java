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

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.message.AnalysisIdentifier;
import bio.overture.maestro.domain.api.message.IndexAnalysisCommand;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.RemoveAnalysisCommand;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import reactor.core.publisher.Mono;

import static bio.overture.maestro.app.infra.adapter.inbound.messaging.IndexMessagesHelper.handleIndexResult;

@Slf4j
@EnableBinding(SongAnalysisSink.class)
public class SongAnalysisStreamListener {

    private static final String PUBLISHED = "PUBLISHED";

    private Indexer indexer;

    public SongAnalysisStreamListener(Indexer indexer) {
        this.indexer = indexer;
    }

    @StreamListener(SongAnalysisSink.NAME)
    public void handleMessage(@Payload AnalysisMessage analysisMessage) {
        log.info("received message : {}", analysisMessage);
        handleIndexResult(() -> this.doHandle(analysisMessage));
    }

    private Mono<Tuple2<AnalysisMessage, IndexResult>> doHandle(AnalysisMessage msg) {
        Mono<IndexResult> resultMono;
        try {
            if (msg.getState().equals(PUBLISHED)) {
                resultMono = indexer.indexAnalysis(IndexAnalysisCommand.builder()
                    .analysisIdentifier(AnalysisIdentifier.builder()
                        .repositoryCode(msg.getSongServerId())
                        .studyId(msg.getStudyId())
                        .analysisId(msg.getAnalysisId())
                        .build())
                    .build());
            } else { // UNPUBLISHED, SUPPRESSED
                resultMono = indexer.removeAnalysis(RemoveAnalysisCommand.builder()
                    .analysisIdentifier(AnalysisIdentifier.builder()
                        .analysisId(msg.getAnalysisId())
                        .studyId(msg.getStudyId())
                        .repositoryCode(msg.getSongServerId())
                        .build()
                    ).build());
            }
            return resultMono.map(indexResult -> new Tuple2<>(msg, indexResult));
        } catch (Exception e) {
            log.error("failed reading message: {} ", msg, e);

            return Mono.just(new Tuple2<>(msg, IndexResult.builder().successful(false).build()));
        }
    }
}
