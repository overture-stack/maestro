package bio.overture.maestro.domain.api;

import static bio.overture.maestro.domain.api.exception.NotFoundException.checkNotFound;

import bio.overture.maestro.domain.entities.indexing.Donor;
import bio.overture.maestro.domain.entities.indexing.Specimen;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Sample;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.val;

final class DocumentConverterHelper {
  /** Converts metadata Specimen pojo and metadata Sample pojo to an indexing Specimen pojo. */
  static List<Specimen> buildSpecimen(
      @NonNull bio.overture.maestro.domain.entities.metadata.study.Specimen specimen,
      @NonNull Sample sample) {

    val sampleDoc =
        bio.overture.maestro.domain.entities.indexing.Sample.builder()
            .sampleId(sample.getSampleId())
            .matchedNormalSubmitterSampleId(sample.getMatchedNormalSubmitterSampleId())
            .submitterSampleId(sample.getSubmitterSampleId())
            .sampleType(sample.getSampleType())
            .build();
    sampleDoc.replaceInfo(sample.getInfo());

    val specimenDoc =
        bio.overture.maestro.domain.entities.indexing.Specimen.builder()
            .specimenId(specimen.getSpecimenId())
            .submitterSpecimenId(specimen.getSubmitterSpecimenId())
            .specimenType(specimen.getSpecimenType())
            .specimenTissueSource(specimen.getSpecimenTissueSource())
            .tumourNormalDesignation(specimen.getTumourNormalDesignation())
            .samples(List.of(sampleDoc))
            .build();
    specimenDoc.replaceInfo(specimen.getInfo());
    return List.of(specimenDoc);
  }

  /**
   * Converts song metadata sample to AnalysisCentricDonor, each song Sample has one donor and one
   * specimen.
   *
   * @param sample song metadata Sample object
   * @return converted AnalysisCentricDonor object
   */
  static Donor extractDonor(@NonNull Sample sample) {
    val donor = sample.getDonor();
    val specimen = sample.getSpecimen();
    val donorDoc =
        Donor.builder()
            .donorId(donor.getDonorId())
            .gender(donor.getGender())
            .submitterDonorId(donor.getSubmitterDonorId())
            .specimens(buildSpecimen(specimen, sample))
            .build();
    donorDoc.replaceInfo(donor.getInfo());
    return donorDoc;
  }

  /**
   * Groups specimens belonging to a AnalysisCentricDonor
   *
   * @param list a grouped list with each AnalysisCentricDonor element having exactly one Specimen
   * @return a fully assembled Donor object with a list of specimens that belongs to the current
   *     donor
   */
  static Donor mergeDonorBySpecimen(@NonNull List<Donor> list) {
    checkNotFound(
        list.size() > 0, "Failed to merge FileCentricDonor by specimen: donor list is empty.");

    // Every element in list has the same donor, so just use the first donor
    val anyDonor = list.get(0);

    checkNotFound(
        anyDonor.getSpecimens() != null && anyDonor.getSpecimens().size() > 0,
        "Failed to merge FileCentricDonor by specimen: donor doesn't have specimen,");

    val specimenList =
        list.stream()
            .map(fileCentricDonor -> fileCentricDonor.getSpecimens().get(0))
            .collect(Collectors.toList());

    val specimenMap =
        specimenList.stream()
            .collect(Collectors.groupingBy(Specimen::getSpecimenId, Collectors.toList()));

    val specimens =
        new ArrayList<>(specimenMap.values())
            .stream()
                .map(DocumentConverterHelper::groupSpecimensBySample)
                .collect(Collectors.toList());

    val donorDoc =
        Donor.builder()
            .donorId(anyDonor.getDonorId())
            .submitterDonorId(anyDonor.getSubmitterDonorId())
            .gender(anyDonor.getGender())
            .specimens(specimens)
            .build();

    donorDoc.replaceInfo(anyDonor.getInfo());
    return donorDoc;
  }

  static bio.overture.maestro.domain.entities.indexing.Specimen groupSpecimensBySample(
      @NonNull List<bio.overture.maestro.domain.entities.indexing.Specimen> list) {
    checkNotFound(list.size() > 0, "Failed to merge Specimen by Sample: Specimen list is empty.");

    val samples = new ArrayList<bio.overture.maestro.domain.entities.indexing.Sample>();
    list.forEach(specimen -> samples.addAll(specimen.getSamples()));
    val specimen = list.get(0);

    // if there is more than one sample in the list, merge samples under one specimen
    if (list.size() > 1) {
      val specimenDoc =
          bio.overture.maestro.domain.entities.indexing.Specimen.builder()
              .samples(samples)
              .specimenId(specimen.getSpecimenId())
              .specimenType(specimen.getSpecimenType())
              .tumourNormalDesignation(specimen.getTumourNormalDesignation())
              .specimenTissueSource(specimen.getSpecimenTissueSource())
              .submitterSpecimenId(specimen.getSubmitterSpecimenId())
              .build();
      specimenDoc.replaceInfo(specimen.getInfo());
      return specimenDoc;
    } else return specimen;
  }

  static List<Donor> getDonors(@NonNull Analysis analysis) {
    val groupedByDonorMap =
        analysis.getSamples().stream()
            .map(DocumentConverterHelper::extractDonor)
            .collect(Collectors.groupingBy(Donor::getDonorId, Collectors.toList()));

    return new ArrayList<>(groupedByDonorMap.values())
        .stream().map(DocumentConverterHelper::mergeDonorBySpecimen).collect(Collectors.toList());
  }
}
