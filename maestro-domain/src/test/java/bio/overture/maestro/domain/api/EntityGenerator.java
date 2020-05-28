package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.entities.indexing.Sample;
import bio.overture.maestro.domain.entities.indexing.Specimen;
import lombok.val;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for AnalysisCentricDocumentConverterTest and
 * FileCentricDocumentConverterTest.
 */
public class EntityGenerator {
  public static List<Specimen> buildSpecimenListForDonor() {
    val specimen = Specimen.builder()
        .specimenId("SP1")
        .specimenTissueSource("Other")
        .submitterSpecimenId("MDT-AP-0749_tumor_specimen")
        .tumourNormalDesignation("Tumour")
        .specimenType("Primary tumour - solid tissue")
        .samples(List.of(
            Sample.builder()
                .id("SA1")
                .sampleType("DNA")
                .submitterSampleId("MDT-AP-0749_tumor")
                .matchedNormalSubmitterSampleId("PCSI_0216_St_R")
                .build()))
        .build();
    return List.of(specimen);
  }

  public static List<Specimen> buildSpecimenListForDonor1() {
    val specimen_1 = Specimen.builder()
        .specimenId("SP1")
        .specimenTissueSource("Other")
        .submitterSpecimenId("MDT-AP-0749_tumor_specimen")
        .tumourNormalDesignation("Tumour")
        .specimenType("Primary tumour - solid tissue")
        .samples(List.of(
            Sample.builder()
                .id("SA1")
                .sampleType("DNA")
                .submitterSampleId("MDT-AP-0749_tumor")
                .matchedNormalSubmitterSampleId("PCSI_0216_St_R")
                .build()))
        .build();

    val specimen_2 = Specimen.builder()
        .specimenId("SP2")
        .specimenTissueSource("Other")
        .submitterSpecimenId("MDT-AP-0749_tumor_specimen")
        .tumourNormalDesignation("Tumour")
        .specimenType("Primary tumour - solid tissue")
        .samples(List.of(
            Sample.builder()
                .id("SA2")
                .sampleType("DNA")
                .submitterSampleId("MDT-AP-0749_tumor")
                .matchedNormalSubmitterSampleId("PCSI_0216_St_R")
                .build()))
        .build();

    val list = new ArrayList<Specimen>();
    list.add(specimen_2);
    list.add(specimen_1);
    return list;
  }

  public static List<Specimen> buildSpecimenListForDonor2() {
    val specimen_3 = Specimen.builder()
        .specimenId("SP3")
        .specimenTissueSource("Other")
        .submitterSpecimenId("MDT-AP-0749_tumor_specimen")
        .tumourNormalDesignation("Tumour")
        .specimenType("Primary tumour - solid tissue")
        .samples(buuldSamplesForDonor2_sp3())
        .build();
    val specimen_4 = Specimen.builder()
        .specimenId("SP4")
        .specimenTissueSource("Other")
        .submitterSpecimenId("MDT-AP-0749_tumor_specimen")
        .tumourNormalDesignation("Tumour")
        .specimenType("Primary tumour - solid tissue")
        .samples(buildSamplesForDonor2_sp4())
        .build();

    val list = new ArrayList<Specimen>();
    list.add(specimen_4);
    list.add(specimen_3);
    return list;
  }

  public static List<Sample> buuldSamplesForDonor2_sp3() {
    val sample_3 = Sample.builder()
        .id("SA3")
        .sampleType("DNA")
        .submitterSampleId("MDT-AP-0749_tumor")
        .matchedNormalSubmitterSampleId("PCSI_0216_St_R")
        .build();
    val sample_4 = Sample.builder()
        .id("SA4")
        .sampleType("DNA")
        .submitterSampleId("MDT-AP-0749_tumor")
        .matchedNormalSubmitterSampleId("PCSI_0216_St_R")
        .build();
    return List.of(sample_3, sample_4);
  }

  public static List<Sample> buildSamplesForDonor2_sp4() {
    val sample_5 = Sample.builder()
        .id("SA5")
        .sampleType("DNA")
        .submitterSampleId("MDT-AP-0749_tumor")
        .matchedNormalSubmitterSampleId("PCSI_0216_St_R")
        .build();
    val sample_6 = Sample.builder()
        .id("SA6")
        .sampleType("DNA")
        .submitterSampleId("MDT-AP-0749_tumor")
        .matchedNormalSubmitterSampleId("PCSI_0216_St_R")
        .build();
    return List.of(sample_5, sample_6);
  }
}
