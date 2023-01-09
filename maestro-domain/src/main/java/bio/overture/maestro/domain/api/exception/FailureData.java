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

package bio.overture.maestro.domain.api.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class FailureData {
  @Builder.Default private final Map<String, Set<String>> failingIds = new HashMap<>();

  private void addFailures(String type, Set<String> ids) {
    if (failingIds.containsKey(type)) {
      failingIds.put(
          type,
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
