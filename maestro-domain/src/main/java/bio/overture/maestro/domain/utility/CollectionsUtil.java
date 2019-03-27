package bio.overture.maestro.domain.utility;

import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

@UtilityClass
public class CollectionsUtil {
    public static <T> Map<Integer, List<T>> partitionList(List<T> list, int partSize) {
        return IntStream.iterate(0, i -> i + partSize)
            .limit((list.size() + partSize - 1) / partSize)
            .boxed()
            .collect(toMap(i -> i / partSize,
                i -> list.subList(i, min(i + partSize, list.size()))));
    }
}
