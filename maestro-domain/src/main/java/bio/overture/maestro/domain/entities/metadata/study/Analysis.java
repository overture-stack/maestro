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
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.*;

/** This corresponds to the analysis entity in the file metadata repository. */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Analysis {

  @NonNull @ExclusionId private String analysisId;

  /** Method used in this analysis (variantCall, sequenceRead) */
  @NonNull private AnalysisTypeId analysisType;

  /** the status of the analysis (published or other values) */
  @NonNull private String analysisState;

  /** the studyId Id that this analysis belongs to. */
  @NonNull private String studyId;

  /** multiple files belong to an analysis, files can be related (bam, bai, xml) */
  @NonNull private List<File> files;

  /** An analysis can have one or more samples */
  @NonNull private List<Sample> samples;

  /**
   * extra information about the analysis type. this will contain attributes that change by the
   * analysis type.
   *
   * <p>see the source repository api for more info if needed.
   */
  private Map<String, Object> experiment;

  /** data represents song dynamic fields. */
  @NonNull private final Map<String, Object> data = new TreeMap<>();

  @JsonAnyGetter
  public Map<String, Object> getData() {
    return data;
  }

  @JsonAnySetter
  public void setData(String key, Object value) {
    data.put(key, value);
  }
}
