package bio.overture.maestro.domain.utility;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.utility.CollectionsUtil.partitionList;

@Slf4j
@UtilityClass
public final class Parallel {

    /**
     * A BLOCKING (i.e. should be executed on a reactor scheduler) scatter gather to parallelize execution
     *  of suppliers, currently it uses completeable futures but may be transformed to reactor publishers.
     *  it handles paritioning an input list to a batch and create the necessary number of workers.
     *  it uses the default completeable future work stealing ForkJoin pool.
     *
     * @param inputList the list of input parameters that we want to split and pass to the supplier
     * @param batchSize the size of the batch each supplier will handler
     * @param supplier the function to take an input batch and supply the result
     * @param <I> the type parameter for the inputs
     * @param <R> the return type of the results
     * @return a list of R
     * @throws Exception this is here to force callers to handle the exceptions that can be thrown
     *          this method will only rethrow any exceptiona and fails fast.
     *          if another behaviour is desired suppliers should handle their exceptions.
     */
    @SneakyThrows
    public static <I, R> List<R> blockingScatterGather(List<I> inputList, int batchSize,
          Function<Map.Entry<Integer, List<I>>, R> supplier) throws Exception {
        // scatter
        val futures = partitionList(inputList, batchSize).entrySet().stream()
            .map((entry) -> CompletableFuture.supplyAsync(() -> supplier.apply(entry)))
            .collect(Collectors.toUnmodifiableList());

        //gather
        val joinedFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v-> futures.stream().map(CompletableFuture::join).collect(Collectors.toUnmodifiableList()));

        try {
            return joinedFutures.get();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

}
