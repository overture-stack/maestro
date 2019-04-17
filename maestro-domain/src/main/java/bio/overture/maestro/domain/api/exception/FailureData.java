package bio.overture.maestro.domain.api.exception;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class FailureData {
    @Builder.Default
    private final Map<String, Set<String>> failingIds = new HashMap<>();

    private void addFailures(String type, Set<String> ids) {
        if (failingIds.containsKey(type)) {
            failingIds.put(type,
                Stream.concat(failingIds.get(type).stream(), ids.stream())
                    .collect(Collectors.toUnmodifiableSet()));
            return;
        }
        failingIds.put(type, Set.copyOf(ids));
    }

    public Map<String, Set<String>> getFailingIds() {
        return Map.copyOf(this.failingIds);
    }

    public void addFailures(FailureData failureData) {
        failureData.getFailingIds().forEach(this::addFailures);
    }
}
