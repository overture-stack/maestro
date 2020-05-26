package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.entities.indexing.Sample;
import bio.overture.maestro.domain.entities.indexing.Specimen;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDonor;
import bio.overture.maestro.domain.entities.metadata.study.*;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;

import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static bio.overture.masestro.test.TestCategory.UNIT_TEST;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@Tag(UNIT_TEST)
public class AnalysisCentricDocumentConverterTest {

  @Test
  void testGetDonor() {
    val analysisObj = loadJsonFixture(this.getClass(), "TEST-CA.analysis.json", Analysis.class);

    // expected:
    val donor = AnalysisCentricDonor.builder()
        .id("DO1")
        .gender("Female")
        .submitterDonorId("MDT-AP-0749")
        .specimens(buildSpecimenListForDonor())
        .build();

    val results = AnalysisCentricDocumentConverter.getDonors(analysisObj);

    assertEquals(1, results.size());
    assertEquals(donor, results.get(0));
  }

  @Test
  void testGetDonors_multi_donor(){
    // Expected AnalysisCentricDonor data structure:
    // Analysis  => d1 -> sp1 -> [sa1]
    //              d1 -> sp2 -> [sa2]
    //              d2 -> sp3 -> [sa3, sa4]
    //              d2 -> sp4 -> [sa5, sa6]
    val analysisObj = loadJsonFixture(this.getClass(),
            "TEST-CA.analysis.multi-donor.json", Analysis.class);

    // expected results:
    val donor_1 = AnalysisCentricDonor.builder()
            .id("DO1")
            .gender("Female")
            .submitterDonorId("MDT-AP-0749")
            .specimens(buildSpecimenListForDonor1())
            .build();

    val donor_2 = AnalysisCentricDonor.builder()
            .id("DO2")
            .gender("Female")
            .submitterDonorId("MDT-AP-0749")
            .specimens(buildSpecimenListForDonor2())
            .build();

    val results = AnalysisCentricDocumentConverter.getDonors(analysisObj);

    assertNotNull(results);
    assertEquals(2, results.size());
    assertEquals(donor_2, results.get(0));
    assertEquals(donor_1, results.get(1));
  }

  private List<Specimen> buildSpecimenListForDonor() {
    val specimen = Specimen.builder()
        .id("SP1")
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

  private List<Specimen> buildSpecimenListForDonor1() {
    val specimen_1 = Specimen.builder()
            .id("SP1")
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
            .id("SP2")
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

  private List<Specimen> buildSpecimenListForDonor2() {
    val specimen_3 = Specimen.builder()
            .id("SP3")
            .specimenTissueSource("Other")
            .submitterSpecimenId("MDT-AP-0749_tumor_specimen")
            .tumourNormalDesignation("Tumour")
            .specimenType("Primary tumour - solid tissue")
            .samples(buuldSamplesForDonor2_sp3())
            .build();
    val specimen_4 = Specimen.builder()
            .id("SP4")
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

  private List<Sample> buuldSamplesForDonor2_sp3() {
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

  private List<Sample> buildSamplesForDonor2_sp4() {
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
