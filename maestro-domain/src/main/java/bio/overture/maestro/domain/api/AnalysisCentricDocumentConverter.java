package bio.overture.maestro.domain.api;

import static bio.overture.maestro.domain.api.DocumentConverterHelper.getDonors;

import bio.overture.maestro.domain.entities.indexing.Repository;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDocument;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricFile;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.File;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@UtilityClass
final class AnalysisCentricDocumentConverter {

  static List<AnalysisCentricDocument> fromAnalysis(Analysis analysis, StudyRepository repository) {
    return List.of(convertAnalysis(analysis, repository));
  }

  static AnalysisCentricDocument convertAnalysis(Analysis analysis, StudyRepository repository) {
    val doc =
        AnalysisCentricDocument.builder()
            .analysisId(analysis.getAnalysisId())
            .analysisState(analysis.getAnalysisState())
            .publishedAt(analysis.getPublishedAt())
            .updatedAt(analysis.getUpdatedAt())
            .firstPublishedAt(analysis.getFirstPublishedAt())
            .analysisType(analysis.getAnalysisType().getName())
            .analysisVersion(analysis.getAnalysisType().getVersion())
            .studyId(analysis.getStudyId())
            .donors(getDonors(analysis))
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
            .experiment(analysis.getExperiment())
            .files(buildAnalysisCentricFiles(analysis.getFiles()))
            .build();
    doc.replaceData(analysis.getData());
    return doc;
  }

  private static List<AnalysisCentricFile> buildAnalysisCentricFiles(@NonNull List<File> files) {
    return files.stream()
        .map(AnalysisCentricDocumentConverter::fromFile)
        .collect(Collectors.toList());
  }

  private static AnalysisCentricFile fromFile(@NonNull File file) {
    val fileDoc =
        AnalysisCentricFile.builder()
            .objectId(file.getObjectId())
            .fileAccess(file.getFileAccess())
            .dataType(file.getDataType())
            .md5Sum(file.getFileMd5sum())
            .name(file.getFileName())
            .size(file.getFileSize())
            .fileType(file.getFileType())
            .build();
    fileDoc.replaceInfo(file.getInfo());
    return fileDoc;
  }
}
