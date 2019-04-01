package bio.overture.maestro.domain.entities.indexing.rules;


import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IDExclusionRule extends ExclusionRule {

    private Class<?> clazz;
    private List<Object> ids = new ArrayList<>();

    @SneakyThrows
    public <T> boolean applies(T instance) {
        if (!instance.getClass().equals(clazz)) return false;
        val idExclusionField = Arrays.stream(instance.getClass().getDeclaredFields())
            .filter(field -> field.getAnnotationsByType(ExclusionId.class).length > 0)
            .findFirst()
            .orElse(null);

        if (idExclusionField == null) {
            return false;
        }

        val value = idExclusionField.get(instance);
        if (value == null) return false;
        return ids.contains(value);
    }
}
