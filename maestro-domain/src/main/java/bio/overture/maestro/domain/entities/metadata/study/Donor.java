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

package bio.overture.maestro.domain.entities.metadata.study;

import bio.overture.maestro.domain.entities.indexing.rules.ExclusionId;
import java.util.Map;
import lombok.*;

/** This represents the samples donor A donor is part of the {@link Sample} entity. */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Donor {
  /** The id of this donor */
  @NonNull @ExclusionId private String donorId;

  /** the id as submitted by the analysis creator */
  @NonNull private String submitterDonorId;

  /** the studyId which this donor belongs to */
  @NonNull private String studyId;

  /** can be Male, Female, Other */
  @NonNull private String gender;

  /** for extra information if any. */
  private Map<String, Object> info;
}
