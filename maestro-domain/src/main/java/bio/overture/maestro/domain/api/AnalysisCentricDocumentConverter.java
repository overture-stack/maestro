package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.entities.indexing.AnalysisType;
import bio.overture.maestro.domain.entities.indexing.Repository;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDocument;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDonor;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricFile;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.File;
import bio.overture.maestro.domain.entities.metadata.study.Sample;
import bio.overture.maestro.domain.entities.metadata.study.Specimen;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import java.util.List;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.api.FileCentricDocumentConverter.getMetadataFileId;
import static bio.overture.maestro.domain.api.exception.NotFoundException.checkNotFound;

@Slf4j
@UtilityClass
final class AnalysisCentricDocumentConverter {

  static List<AnalysisCentricDocument> fromAnalysis(Analysis analysis, StudyRepository repository) {
    return List.of(convertAnalysis(analysis, repository));
  }

  static AnalysisCentricDocument convertAnalysis(Analysis analysis, StudyRepository repository){
    val metadataFileId = getMetadataFileId(analysis);
    return AnalysisCentricDocument.builder()
            .analysisId(analysis.getAnalysisId())
            .analysisState(analysis.getAnalysisState())
            .analysisType(AnalysisType.builder()
                    .name(analysis.getAnalysisType().getName())
                    .version(analysis.getAnalysisType().getVersion())
                    .build())
            .studyId(analysis.getStudyId())
            .donors(getDonors(analysis))
            .repositories(List.of(Repository.builder()
                    .type(repository.getStorageType().name().toUpperCase())
                    .organization(repository.getOrganization())
                    .name(repository.getName())
                    .code(repository.getCode())
                    .country(repository.getCountry())
                    .url(repository.getUrl())
                    .dataPath(repository.getDataPath())
                    .metadataPath(repository.getMetadataPath() + "/" + metadataFileId)
                    .build()))
            .experiment(analysis.getExperiment())
            .files(buildAnalysisCentricFiles(analysis.getFiles()))
            .build();
  }

  public static List<AnalysisCentricDonor> getDonors(@NonNull Analysis analysis){
    val groupedByDonorMap = analysis.getSamples()
            .stream()
            .map(sample -> extractDonor(sample))
            .collect(Collectors.groupingBy(AnalysisCentricDonor :: getId, Collectors.toList()));

    return groupedByDonorMap.values()
            .stream()
            .collect(Collectors.toList())
            .stream()
            .map(donorList -> mergeDonorBySpecimen(donorList))
            .collect(Collectors.toList());
  }

  /**
   * Groups specimens belonging to a AnalysisCentricDonor
   * @param list a grouped list with each AnalysisCentricDonor element having exactly one Specimen
   * @return a fully assembled AnalysisCentticDonor object with a list of specimens that belongs to the current donor
   */
  private static AnalysisCentricDonor mergeDonorBySpecimen(@NonNull List<AnalysisCentricDonor> list){

    checkNotFound(list.size() > 0,
            "Failed to merge AnalysisCentricDonor by specimen: donor list is empty.");

    // Every element in list has the same donor, so just use the first donor
    val anyDonor = list.get(0);

    checkNotFound(anyDonor.getSpecimen() != null && anyDonor.getSpecimen().size() > 0,
            "Failed to merge AnalysisCentricDonor by specimen: donor doesn't have specimen,");

    val specimenList = list.stream()
            // One donor only has one specimen in list
            .map(analysisCentricDonor -> analysisCentricDonor.getSpecimen().get(0))
            .collect(Collectors.toList());

    return AnalysisCentricDonor.builder()
            .id(anyDonor.getId())
            .submittedId(anyDonor.getSubmittedId())
            .gender(anyDonor.getGender())
            .specimen(specimenList)
            .build();
  }

  private static AnalysisCentricDonor extractDonor(@NonNull Sample sample){
    val donor = sample.getDonor();
    val specimen = sample.getSpecimen();
    return AnalysisCentricDonor.builder()
            .id(donor.getDonorId())
            .gender(donor.getGender())
            .submittedId(donor.getSubmitterDonorId())
            .specimen(buildSpecimen(specimen, sample))
            .build();
  }

  /**
   * Converts metadata Specimen pojo and metadata Sample pojo to an indexing Specimen pojo.
   * @param specimen
   * @param sample
   * @return
   */
  private static List<bio.overture.maestro.domain.entities.indexing.Specimen> buildSpecimen(@NonNull Specimen specimen,
                                                                                            @NonNull Sample sample){
    return List.of(bio.overture.maestro.domain.entities.indexing.Specimen.builder()
            .id(specimen.getSpecimenId())
            .submitterSpecimenId(specimen.getSubmitterSpecimenId())
            .specimenType(specimen.getSpecimenType())
            .specimenTissueSource(specimen.getSpecimenTissueSource())
            .tumourNormalDesignation(specimen.getTumourNormalDesignation())
            .samples(bio.overture.maestro.domain.entities.indexing.Sample.builder()
                .id(sample.getSampleId())
                .matchedNormalSubmitterSampleId(sample.getMatchedNormalSubmitterSampleId())
                .submitterSampleId(sample.getSubmitterSampleId())
                .sampleType(sample.getSampleType())
                .build())
            .build());
  }

  private static List<AnalysisCentricFile> buildAnalysisCentricFiles(@NonNull List<File> files){
    return files.stream()
            .map(file -> fromFile(file))
            .collect(Collectors.toList());
  }

  private static AnalysisCentricFile fromFile(@NonNull File file){
    return AnalysisCentricFile.builder()
            .id(file.getObjectId())
            .access(file.getFileAccess())
            .dataType(file.getDataType())
            .md5Sum(file.getFileMd5sum())
            .name(file.getFileName())
            .size(file.getFileSize())
            .type(file.getFileType())
            .build();
  }
}
