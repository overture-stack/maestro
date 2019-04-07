package bio.overture.maestro.domain.entities.indexing.rules;


import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * When applied on an instance it checks if the field annotated with {@link ExclusionId}
 * is in the list of ids that should be excluded.
 *
 * if multiple fields in the instance marked with this, first one (as returned in the class metadata) wins
 */
@Slf4j
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IDExclusionRule extends ExclusionRule {

    /**
     * the class this rules applies to.
     */
    private Class<?> clazz;

    /**
     * the list of ids to be excluded.
     */
    private List<String> ids = new ArrayList<>();

    @SneakyThrows
    public boolean applies(Object instance) {
        log.debug("checking rule against : {}", instance);
        if (ids.isEmpty() || !instance.getClass().equals(clazz)) return false;

        val idExclusionField = Arrays.stream(instance.getClass().getDeclaredFields())
            .filter(field -> field.getAnnotationsByType(ExclusionId.class).length > 0)
            .findFirst()
            .orElse(null);

        if (idExclusionField == null) {
            log.trace("idExclusionField is null");
            return false;
        }

        idExclusionField.setAccessible(true);
        val value = idExclusionField.get(instance);
        if (value == null) {
            log.trace("value is null is null");
            return false;
        }

        val excluded = ids.contains(String.valueOf(value));
        log.debug("id exclusion rule fo value {} = {}", value, excluded);
        return excluded;
    }

}
