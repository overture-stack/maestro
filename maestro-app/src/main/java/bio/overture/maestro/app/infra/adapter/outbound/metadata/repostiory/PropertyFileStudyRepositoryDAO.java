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

package bio.overture.maestro.app.infra.adapter.outbound.metadata.repostiory;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.app.infra.config.properties.PropertiesFileRepository;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.port.outbound.metadata.repository.StudyRepositoryDAO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.List;

import static bio.overture.maestro.domain.utility.Exceptions.notFound;

/**
 * Properties files backed repository store, reads the information by binding
 * to the application.config files property: maestro.repositories
 * the configurable attributes can be found here: {@link PropertiesFileRepository}
 *
 * this serves as a default in memory store, more sophisticated cases may create a DB backed store.
 */
@Slf4j
@Getter
@NoArgsConstructor
class PropertyFileStudyRepositoryDAO implements StudyRepositoryDAO {

    private static final String MSG_REPO_NOT_FOUND = "Repository {0} not found";
    private List<PropertiesFileRepository> repositories;

    @Inject
    public PropertyFileStudyRepositoryDAO(ApplicationProperties properties) {
        this.repositories = properties.repositories();
    }

    @Override
    @NonNull
    public Mono<StudyRepository> getFilesRepository(@NonNull String code) {
        val repository = repositories.stream()
            .filter(propertiesFileRepository -> propertiesFileRepository.getCode().equalsIgnoreCase(code))
            .distinct()
            .map(this :: toFilesRepository)
            .findFirst()
            .orElse(null);
        if (repository == null) {
            return Mono.error(notFound(MSG_REPO_NOT_FOUND, code));
        }
        log.debug("loaded repository : {}", repository);
        return Mono.just(repository);
    }

    private StudyRepository toFilesRepository(PropertiesFileRepository propertiesFileRepository) {
        log.trace("Converting : {} to StudyRepository ", propertiesFileRepository);
        return StudyRepository.builder()
            .code(propertiesFileRepository.getCode())
            .url(propertiesFileRepository.getUrl())
            .name(propertiesFileRepository.getName())
            .organization(propertiesFileRepository.getOrganization())
            .country(propertiesFileRepository.getCountry())
            .storageType(propertiesFileRepository.getStorageType())
            .build();
    }

}
