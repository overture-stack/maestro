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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.*;
import lombok.experimental.FieldNameConstants;

/** Represents the structure of the index document that corresponds to an analysis "File". */
@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants
public class FileCentricDocument {

  @NonNull private String objectId;

  @NonNull private String studyId;

  private String dataType;

  private String fileType;

  private String fileAccess;

  @NonNull private FileCentricAnalysis analysis;

  /** The actual genome analysis files information. */
  @NonNull private File file;

  /**
   * Each files can be hosted in more than one files repository, this references the other
   * repositories (locations) where this files can be fetched from.
   */
  @NonNull private List<Repository> repositories;

  @NonNull private List<Donor> donors;

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
  /**
   * This method is to check if the files is a valid replica of another files. by replication we
   * mean that an analysis can be copied to a different metadata repository to make downloading the
   * files faster for different geographical locations. it checks all attributes except for the
   * repository (since the repository is expected to be different)
   *
   * @param fileCentricDocument the other files we compare to
   * @return flag indicates if this is a valid replica.
   */
  public boolean isValidReplica(FileCentricDocument fileCentricDocument) {
    if (fileCentricDocument == null) return false;
    if (this.equals(fileCentricDocument)) return true;
    return this.objectId.equals(fileCentricDocument.getObjectId())
        && this.studyId.equals(fileCentricDocument.getStudyId())
        && this.dataType.equals(fileCentricDocument.getDataType())
        && this.fileType.equals(fileCentricDocument.getFileType())
        && this.fileAccess.equals(fileCentricDocument.getFileAccess())
        && this.donors.equals(fileCentricDocument.getDonors())
        // FIXME: Might need a rethink of the replica problem as we need to be able to upsert more fields
        && this.analysis.getAnalysisId().equals(fileCentricDocument.getAnalysis().getAnalysisId())
        && this.analysis.getAnalysisType().equals(fileCentricDocument.getAnalysis().getAnalysisType())
        && this.file.equals(fileCentricDocument.getFile());
  }
}
