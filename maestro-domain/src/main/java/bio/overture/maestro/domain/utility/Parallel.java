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

package bio.overture.maestro.domain.utility;

import static bio.overture.maestro.domain.utility.CollectionsUtil.partitionList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@UtilityClass
public final class Parallel {

  /**
   * A BLOCKING (i.e. should be executed on a reactor scheduler) scatter gather to parallelize
   * execution of suppliers, currently it uses completeable futures but may be transformed to
   * reactor publishers. it handles paritioning an input list to a batch and create the necessary
   * number of workers. it uses the default completeable future work stealing ForkJoin pool.
   *
   * @param inputList the list of input parameters that we want to split and pass to the supplier
   * @param batchSize the size of the batch each supplier will handler
   * @param supplier the function to take an input batch and supply the result
   * @param <I> the type parameter for the inputs
   * @param <R> the return type of the results
   * @return a list of R
   * @throws Exception this is here to force callers to handle the exceptions that can be thrown
   *     this method will only rethrow any exceptiona and fails fast. if another behaviour is
   *     desired suppliers should handle their exceptions.
   */
  @SneakyThrows
  public static <I, R> List<R> blockingScatterGather(
      List<I> inputList, int batchSize, Function<Map.Entry<Integer, List<I>>, R> supplier)
      throws Exception {
    // scatter
    val futures =
        partitionList(inputList, batchSize).entrySet().stream()
            .map((entry) -> CompletableFuture.supplyAsync(() -> supplier.apply(entry)))
            .collect(Collectors.toUnmodifiableList());

    // gather
    val joinedFutures =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(
                v ->
                    futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toUnmodifiableList()));

    try {
      return joinedFutures.get();
    } catch (Exception e) {
      log.error(e.getMessage());
      throw e;
    }
  }
}
