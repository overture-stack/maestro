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

package bio.overture.maestro.domain.port.outbound.indexing;

import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import reactor.core.publisher.Mono;

/**
 * Adapter for the indexing server client, this provides the indexer with needed APIs to index files
 * centric documents in the index server
 */
public interface FileCentricIndexAdapter {

  /**
   * Updates a fileDocument repositories field, or indexes the whole document if doesn't exist.
   *
   * @param batchIndexFilesCommand requires the full document to insert if doesn't exist.
   * @return flag indicating if the operation was successful.
   */
  Mono<IndexResult> batchUpsertFileRepositories(
      @NonNull BatchIndexFilesCommand batchIndexFilesCommand);

  /**
   * Batch fetch documents from the index by the specified ids.
   *
   * @param ids a list of ids to fetch documents by from elastic search.
   * @return List contains found documents.
   */
  Mono<List<FileCentricDocument>> fetchByIds(List<String> ids);

  /**
   * Method to delete files documents from the files centric index
   *
   * @param fileCentricDocumentIds the list of files to delete
   * @return indexer exception instance contains the list of failures.
   */
  Mono<Void> removeFiles(Set<String> fileCentricDocumentIds);

  /** Remove all files documents related to the specified analysisId */
  Mono<Void> removeAnalysisFiles(String analysisId);
}
