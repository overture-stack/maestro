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

package bio.overture.maestro.domain.entities.indexing.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * When applied on an instance it checks if the field annotated with {@link ExclusionId} is in the
 * list of ids that should be excluded.
 *
 * <p>if multiple fields in the instance marked with this, first one (as returned in the class
 * metadata) wins
 */
@Slf4j
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IDExclusionRule extends ExclusionRule {

  /** the class this rules applies to. */
  private Class<?> clazz;

  /** the list of ids to be excluded. */
  @Builder.Default private List<String> ids = new ArrayList<>();

  @SneakyThrows
  public boolean applies(Object instance) {
    log.trace("checking rule against : {}", instance);
    if (ids.isEmpty() || !instance.getClass().equals(clazz)) return false;

    val idExclusionField =
        Arrays.stream(instance.getClass().getDeclaredFields())
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
    log.trace("id exclusion rule for value {} = {}", value, excluded);

    if (excluded) {
      log.info("id {} was excluded according to the rules", value);
    }
    return excluded;
  }
}
