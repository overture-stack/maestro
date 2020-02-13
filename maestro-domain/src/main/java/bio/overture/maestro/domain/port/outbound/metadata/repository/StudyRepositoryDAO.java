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

package bio.overture.maestro.domain.port.outbound.metadata.repository;

import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import lombok.NonNull;
import reactor.core.publisher.Mono;

/**
 * Provides access to data about the studies repositories that the indexer should read metadata from to get information
 * like: url, repository storage type, urls etc
 */
public interface StudyRepositoryDAO {
    /**
     * Gets a files repository by code
     * @param code the unique code of the repository
     * @return the repository or empty null if not found
     */
    @NonNull Mono<StudyRepository> getFilesRepository(@NonNull String code);
}
