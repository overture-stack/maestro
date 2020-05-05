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

package bio.overture.maestro.domain.entities.indexing;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.Map;
import java.util.TreeMap;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants
public class FileCentricAnalysis {

    @NonNull
    private String analysisId;
    @NonNull
    private String analysisType;
    @NonNull
    private Integer analysisVersion;
    @NonNull
    private String state;
    @NonNull
    private Map<String, Object> experiment;
    private String studyId;
    /**
     * this field is to capture the dynamic fields in the analysis.
     * it's the responsibility of the users to make sure the mapping is consistent with
     * the different fields that they want to add/index, they are also responsibile
     * to add the mappings of these fields or reindex appropriately.
     */
    @NonNull
    private final Map<String, Object> data = new TreeMap<>();

    @JsonAnyGetter
    public Map<String, Object> getData() {
        return data;
    }

    @JsonAnySetter
    public void setData(String key, Object value) {
        data.put(key, value);
    }

    public void replaceData(Map<String, Object> data) {
        this.data.clear();
        this.data.putAll(data);
    }
}
