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

import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CollectionsUtil {
  public static <T> Map<Integer, List<T>> partitionList(List<T> list, int partSize) {
    return IntStream.iterate(0, i -> i + partSize)
        .limit((list.size() + partSize - 1) / partSize)
        .boxed()
        .collect(toMap(i -> i / partSize, i -> list.subList(i, min(i + partSize, list.size()))));
  }
}
