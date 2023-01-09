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
import java.util.Map;
import java.util.TreeMap;
import lombok.*;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class File {
  @NonNull private String name;
  @NonNull private String md5sum;
  private Long size;
  private String dataType;
  private IndexFile indexFile;
  /**
   * this field is to capture the dynamic fields in the file info. it's the responsibility of the
   * users to make sure the mapping is consistent with the different fields that they want to
   * add/index, they are also responsible to add the mappings of these fields or reindex
   * appropriately.
   */
  @NonNull private final Map<String, Object> info = new TreeMap<>();

  @JsonAnyGetter
  public Map<String, Object> getInfo() {
    return info;
  }

  @JsonAnySetter
  public void setInfo(String key, Object value) {
    info.put(key, value);
  }

  public void replaceInfo(Map<String, Object> data) {
    if (data == null) {
      this.info.clear();
      return;
    }
    this.info.clear();
    this.info.putAll(data);
  }
}
