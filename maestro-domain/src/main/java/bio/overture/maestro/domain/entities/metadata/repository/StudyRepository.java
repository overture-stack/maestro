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

package bio.overture.maestro.domain.entities.metadata.repository;


import bio.overture.maestro.domain.entities.indexing.StorageType;
import lombok.*;

/**
 * This represents a studyId (including analyses & files) metadata repository, holds information about sources where this
 * indexer can pull metadata from.
 */
@Builder
@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class StudyRepository {
    /**
     * display name of the repository
     */
    @NonNull
    private String name;

    /**
     * a unique code for the repository
     */
    @NonNull
    private String code;

    /**
     * the country where this files repository resides
     */
    @NonNull
    private String country;

    /**
     * based url of the host of this repository metadata
     */
    @NonNull
    private String url;

    /**
     * url path to access the files in the object store
     */
    @NonNull
    private String dataPath;

    /**
     * url path to access metadata about the files.
     */
    private String metadataPath;

    /**
     * the block storage type of files (s3 usually)
     */
    @NonNull
    private StorageType storageType;

    /**
     * the organization the owns this files repository
     */
    @NonNull
    private String organization;
}
