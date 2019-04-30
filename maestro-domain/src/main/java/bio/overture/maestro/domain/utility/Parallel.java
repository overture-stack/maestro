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

    @SneakyThrows
    public static <I, R> List<R> scatterGather(List<I> inputList, int batchSize, Function<Map.Entry<Integer, List<I>>, R> supplier) {
        // scatter
        val futures = partitionList(inputList, batchSize).entrySet().stream()
            .map((entry) -> CompletableFuture.supplyAsync(() -> supplier.apply(entry))).collect(Collectors.toUnmodifiableList());
        //gather
        val joinedFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v-> futures.stream().map(CompletableFuture::join).collect(Collectors.toUnmodifiableList()));
        return joinedFutures.get();
    }

}
