package bio.overture.maestro.domain.api;

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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.api.exception.NotFoundException.checkNotFound;

@Slf4j
@UtilityClass
final class AnalysisCentricDocumentConverter {

  static List<AnalysisCentricDocument> fromAnalysis(Analysis analysis, StudyRepository repository) {
    return List.of(convertAnalysis(analysis, repository));
  }

  static AnalysisCentricDocument convertAnalysis(Analysis analysis, StudyRepository repository){
    val doc = AnalysisCentricDocument.builder()
            .analysisId(analysis.getAnalysisId())
            .analysisState(analysis.getAnalysisState())
            .analysisType(analysis.getAnalysisType().getName())
            .analysisVersion(analysis.getAnalysisType().getVersion())
            .studyId(analysis.getStudyId())
            .donors(getDonors(analysis))
            .repositories(List.of(Repository.builder()
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

    checkNotFound(anyDonor.getSpecimens() != null && anyDonor.getSpecimens().size() > 0,
            "Failed to merge AnalysisCentricDonor by specimen: donor doesn't have specimen,");

    //  Each donor has only one specimen in the list
    val specimenList = list.stream()
            .map(analysisCentricDonor -> analysisCentricDonor.getSpecimens().get(0))
            .collect(Collectors.toList());

    val specimenMap = specimenList.stream()
              .collect(
                  Collectors.groupingBy(bio.overture.maestro.domain.entities.indexing.Specimen ::getSpecimenId, Collectors.toList()));

    val specimens = specimenMap.values()
        .stream()
        .collect(Collectors.toList())
        .stream()
        .map(speList -> groupSpecimensBySample(speList))
        .collect(Collectors.toList());

    return AnalysisCentricDonor.builder()
            .id(anyDonor.getId())
            .submitterDonorId(anyDonor.getSubmitterDonorId())
            .gender(anyDonor.getGender())
            .specimens(specimens)
            .build();
  }

  public static bio.overture.maestro.domain.entities.indexing.Specimen groupSpecimensBySample(@NonNull List<bio.overture.maestro.domain.entities.indexing.Specimen> list){
    checkNotFound(list.size() > 0,
        "Failed to merge Specimen by Sample: Specimen list is empty.");

    val samples = new ArrayList<bio.overture.maestro.domain.entities.indexing.Sample>();
    list.stream().forEach(specimen -> samples.addAll(specimen.getSamples()));
    val specimen = list.get(0);

    // if there is more than one sample in the list, merge samples under one specimen
    if(list.size() > 1) {
      return bio.overture.maestro.domain.entities.indexing.Specimen.builder()
          .samples(samples)
          .specimenId(specimen.getSpecimenId())
          .specimenType(specimen.getSpecimenType())
          .tumourNormalDesignation(specimen.getTumourNormalDesignation())
          .specimenTissueSource(specimen.getSpecimenTissueSource())
          .submitterSpecimenId(specimen.getSubmitterSpecimenId())
          .build();
    } else return specimen;
  }

  /**
   * Converts song metadata sample to AnalysisCentricDonor,
   * each song Sample has one donor and one specimen.
   * @param sample song metadata Sample object
   * @return converted AnalysisCentricDonor object
   */
  private static AnalysisCentricDonor extractDonor(@NonNull Sample sample){
    val donor = sample.getDonor();
    val specimen = sample.getSpecimen();
    return AnalysisCentricDonor.builder()
            .id(donor.getDonorId())
            .gender(donor.getGender())
            .submitterDonorId(donor.getSubmitterDonorId())
            .specimens(buildSpecimen(specimen, sample))
            .build();
  }

  /**
   * Converts metadata Specimen pojo and metadata Sample pojo to an indexing Specimen pojo.
   * @param specimen
   * @param sample
   * @return
   */
  public static List<bio.overture.maestro.domain.entities.indexing.Specimen> buildSpecimen(@NonNull Specimen specimen,
                                                                                            @NonNull Sample sample){
    return List.of(bio.overture.maestro.domain.entities.indexing.Specimen.builder()
            .specimenId(specimen.getSpecimenId())
            .submitterSpecimenId(specimen.getSubmitterSpecimenId())
            .specimenType(specimen.getSpecimenType())
            .specimenTissueSource(specimen.getSpecimenTissueSource())
            .tumourNormalDesignation(specimen.getTumourNormalDesignation())
            .samples(List.of(
                bio.overture.maestro.domain.entities.indexing.Sample.builder()
                .sampleId(sample.getSampleId())
                .matchedNormalSubmitterSampleId(sample.getMatchedNormalSubmitterSampleId())
                .submitterSampleId(sample.getSubmitterSampleId())
                .sampleType(sample.getSampleType())
                .build()))
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
            .fileAccess(file.getFileAccess())
            .dataType(file.getDataType())
            .md5Sum(file.getFileMd5sum())
            .name(file.getFileName())
            .size(file.getFileSize())
            .fileType(file.getFileType())
            .build();
  }
}
