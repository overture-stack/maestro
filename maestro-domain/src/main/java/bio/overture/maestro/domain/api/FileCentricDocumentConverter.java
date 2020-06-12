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

package bio.overture.maestro.domain.api;

import static bio.overture.maestro.domain.api.DocumentConverterHelper.getDonors;

import bio.overture.maestro.domain.entities.indexing.*;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * This class holds the structural changes that the indexer applies to prepare the File documents
 * according to the needed final index document structure.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FileCentricDocumentConverter {

  private static final String EMPTY_STRING = "";
  private static final String BAM = "BAM";
  private static final String BAI = "BAI";
  private static final String CRAM = "CRAM";
  private static final String CRAI = "CRAI";
  private static final String XML = "XML";
  private static final String TBI = "TBI";
  private static final String IDX = "IDX";
  private static final String GZ = ".gz";
  private static final String ZIP = ".zip";
  private static final String B_2_ZIP = ".b2zip";
  private static final String VCF = "VCF";
  private static final String TCG = "TCG";
  private static final String IDX_EXT = "." + IDX;
  private static final String TCG_EXT = "." + TCG;
  private static final String BAI_EXT = "." + BAI;
  private static final String CRAI_EXT = "." + CRAI;
  private static final String TBI_EXT = "." + TBI;

  /**
   * Entry point for this converter, it extracts analysis files according to the FileCentricDocument
   * structure this process is per each analysis and there is no effects on other analyses.
   *
   * @param analysis the analysis coming from the studyId metadata source
   * @param repository the repository is needed to add its information to the final document.
   * @return a list of documents each representing files that an analysis produced/used.
   */
  static List<FileCentricDocument> fromAnalysis(Analysis analysis, StudyRepository repository) {
    return extractFiles(analysis, repository);
  }

  /** iterate over the files list of analysis and build a document for each one */
  private static List<FileCentricDocument> extractFiles(
      Analysis analysis, StudyRepository repository) {
    return analysis.getFiles().stream()
        .filter(FileCentricDocumentConverter::isDataFile)
        .map(f -> buildFileDocument(f, analysis, repository))
        .collect(Collectors.toList());
  }

  /**
   * builds the files document from the analysis files
   *
   * @param file a files as represented from the source in the analysis
   */
  private static FileCentricDocument buildFileDocument(
      bio.overture.maestro.domain.entities.metadata.study.File file,
      Analysis analysis,
      StudyRepository repository) {
    val id = file.getObjectId();
    val repoFileBuilder =
        FileCentricDocument.builder()
            .objectId(id)
            .studyId(file.getStudyId())
            .variantClass(analysis.getVariant_class() == null ? null : analysis.getVariant_class())
            .dataType(file.getDataType())
            .fileType(file.getFileType())
            .fileAccess(file.getFileAccess())
            .analysis(
                FileCentricAnalysis.builder()
                    .analysisId(analysis.getAnalysisId())
                    .analysisState(analysis.getAnalysisState())
                    .analysisType(analysis.getAnalysisType().getName())
                    .analysisVersion(analysis.getAnalysisType().getVersion())
                    .experiment(analysis.getExperiment())
                    .build())
            .file(buildGenomeFileInfo(analysis, file))
            .repositories(
                List.of(
                    Repository.builder()
                        .type(repository.getStorageType().name().toUpperCase())
                        .organization(repository.getOrganization())
                        .name(repository.getName())
                        .code(repository.getCode())
                        .country(repository.getCountry())
                        .url(repository.getUrl())
                        .build()))
            .donors(getDonors(analysis));
    val repoFile = repoFileBuilder.build();
    repoFile.getAnalysis().replaceData(analysis.getData());
    repoFile.replaceInfo(file.getInfo());
    return repoFile;
  }

  private static File buildGenomeFileInfo(
      Analysis analysis, bio.overture.maestro.domain.entities.metadata.study.File file) {
    val fileName = file.getFileName();
    val indexFile = getIndexFile(analysis.getFiles(), fileName);
    val fileDocument =
        File.builder()
            .name(fileName)
            .size(file.getFileSize())
            .md5sum(file.getFileMd5sum())
            .dataType(file.getDataType())
            .indexFile(indexFile)
            .build();
    fileDocument.replaceInfo(file.getInfo());
    return fileDocument;
  }

  /**
   * get the index files associated with that files note that this will be removed, when the source
   * explicitly handles associating index files to a files.
   */
  private static IndexFile getIndexFile(
      List<bio.overture.maestro.domain.entities.metadata.study.File> files, String fileName) {

    Optional<bio.overture.maestro.domain.entities.metadata.study.File> sf = Optional.empty();
    if (hasExtension(fileName, BAM)) {
      sf = findIndexFile(files, fileName + BAI_EXT);
    } else if (hasExtension(fileName, CRAM)) {
      sf = findIndexFile(files, fileName + CRAI_EXT);
    } else if (hasExtension(fileName, VCF)) {
      sf =
          Stream.of(TBI_EXT, IDX_EXT, TCG_EXT)
              .map(suffix -> findIndexFile(files, fileName + suffix))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .findFirst();
    }
    return sf.map(FileCentricDocumentConverter::createIndexFile).orElse(null);
  }

  private static IndexFile createIndexFile(
      bio.overture.maestro.domain.entities.metadata.study.File file) {
    val indexFileDocument =
        IndexFile.builder()
            .objectId(file.getObjectId())
            .name(file.getFileName())
            .fileType(indexFileFormat(file.getFileName()))
            .size(file.getFileSize())
            .md5sum(file.getFileMd5sum())
            .dataType(file.getDataType())
            .build();

    indexFileDocument.replaceInfo(file.getInfo());
    return indexFileDocument;
  }

  private static Optional<bio.overture.maestro.domain.entities.metadata.study.File> findIndexFile(
      List<bio.overture.maestro.domain.entities.metadata.study.File> files, String name) {

    return files.stream().filter(f -> f.getFileName().equalsIgnoreCase(name)).findFirst();
  }

  private static boolean isDataFile(bio.overture.maestro.domain.entities.metadata.study.File f) {
    val name = f.getFileName();
    return !(isIndexFile(name) || isXMLFile(name));
  }

  private static boolean isXMLFile(String filename) {
    return hasExtension(filename, XML);
  }

  private static boolean isBAIFile(String filename) {
    return hasExtension(filename, BAI);
  }

  private static boolean isCRAIFile(String filename) {
    return hasExtension(filename, CRAI);
  }

  private static boolean isTBIFile(String filename) {
    return hasExtension(filename, TBI);
  }

  private static boolean isIDXFile(String filename) {
    return hasExtension(filename, IDX);
  }

  private static boolean isIndexFile(String filename) {
    return isBAIFile(filename)
        || isCRAIFile(filename)
        || isIDXFile(filename)
        || isTBIFile(filename);
  }

  private static String indexFileFormat(String fileName) {
    if (isBAIFile(fileName)) {
      return BAI;
    }
    if (isCRAIFile(fileName)) {
      return CRAI;
    }
    if (isTBIFile(fileName)) {
      return TBI;
    }
    if (isIDXFile(fileName)) {
      return IDX;
    }
    return null;
  }

  private static boolean hasExtension(String filename, String extension) {
    String[] suffixes = {EMPTY_STRING, GZ, ZIP, B_2_ZIP};
    val f = filename.toLowerCase();
    val ext = extension.toLowerCase();
    for (val s : suffixes) {
      if (f.endsWith(ext + s)) {
        return true;
      }
      if (f.endsWith(s + ext)) {
        return true;
      }
    }
    return false;
  }
}
