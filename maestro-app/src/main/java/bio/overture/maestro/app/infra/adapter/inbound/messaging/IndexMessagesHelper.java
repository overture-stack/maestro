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

package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import bio.overture.maestro.domain.api.message.IndexResult;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@Slf4j
public class IndexMessagesHelper {
    public static <T> void handleIndexRepository(Supplier<Mono<Tuple2<T, IndexResult>>> resultSupplier) {
        val result = resultSupplier.get().blockOptional();
        val tuple = result.orElseThrow(() -> new RuntimeException("failed to obtain result"));
        if (!tuple._2().isSuccessful()) {
            log.error("failed to process message : {} successfully", tuple._1());
            throw new RuntimeException("failed to process the message");
        }
    }

    public static <T> void handleIndexResult(Supplier<Flux<Tuple2<T, IndexResult>>> resultSupplier) {
        /*
         * Why Blocking?
         *
         * - this is a stream consumer, it's supposed to process one message at a time
         *   the value of reactive processing diminishes since the queue provides a buffering level,
         *   without blocking it will async process the messages and if one fails we can
         *   async add it to a DLQ in the subscriber, However, I opted to use blocking because of the next point.
         *
         * - spring reactive cloud stream is deprecated in favor of spring cloud functions that support
         *   stream processing: https://cloud.spring.io/spring-cloud-static/spring-cloud-stream/2.2.0.RELEASE/spring-cloud-stream.html#spring_cloud_function
         *   so I don't want to use a deprecated library, and if needed we can switch to cloud function in future
         *   https://stackoverflow.com/questions/53438208/spring-cloud-stream-reactive-how-to-do-the-error-handling-in-case-of-reactive
         */
        val result = resultSupplier.get().collectList().blockOptional();
        val tupleList = result.orElseThrow(() -> new RuntimeException("failed to obtain result"));
        tupleList.forEach(tuple -> {
            if (!tuple._2().isSuccessful()) {
                log.error("failed to process message : {} successfully", tuple._1());
                throw new RuntimeException("failed to process the message");
            }
        });
    }
}
