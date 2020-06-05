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

/**
 * A file represents an analysis output that results from the experiment on the Donor specimen.
 * multiple files belong to one Analysis and reside in an object store.
 *
 * <p>A file can reside in multiple repositories and it can have relations to other files in a
 * single analysis like BAM and its index file BAI.
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class File {
  @NonNull @ExclusionId private String objectId;
  @NonNull private String studyId;
  @NonNull private String analysisId;
  @NonNull private String fileName;
  @NonNull private String fileType;
  @NonNull private String fileMd5sum;
  @NonNull private String fileAccess;
  private String dataType;
  private long fileSize;
  private Map<String, Object> info;
}
