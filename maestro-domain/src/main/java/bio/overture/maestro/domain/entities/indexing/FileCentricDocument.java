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

import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.List;

/**
 * Represents the structure of the index document that corresponds to an analysis "File".
 */
@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants
public class FileCentricDocument {

    @NonNull
    private String objectId;

    @NonNull
    private String access;

    @NonNull
    private String studyId;

    @NonNull
    private FileCentricAnalysis analysis;

    /**
     * The actual genome analysis files information.
     */
    @NonNull
    private File files;

    /**
     * Each files can be hosted in more than one files repository, this references the other repositories (locations)
     * where this files can be fetched from.
     */
    @NonNull
    private List<Repository> repositories;

    @NonNull
    private List<FileCentricDonor> donors;

    /**
     * This method is to check if the files is a valid replica of another files.
     * by replication we mean that an analysis can be copied to a different metadata repository to make downloading
     * the files faster for different geographical locations.
     * it checks all attributes except for the repository (since the repository is expected to be different)
     *
     * @param fileCentricDocument the other files we compare to
     * @return flag indicates if this is a valid replica.
     */
    public boolean isValidReplica(FileCentricDocument fileCentricDocument) {
        if (fileCentricDocument == null) return false;
        if (this.equals(fileCentricDocument)) return true;
        return this.objectId.equals(fileCentricDocument.getObjectId())
            && this.access.equals(fileCentricDocument.getAccess())
            && this.studyId.equals(fileCentricDocument.getStudyId())
            && this.donors.equals(fileCentricDocument.getDonors())
            && this.analysis.equals(fileCentricDocument.getAnalysis())
            && this.files.equals(fileCentricDocument.getFiles());
    }
}

