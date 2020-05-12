package bio.overture.maestro.domain.api;

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
  void testGetDonors(){
    val analysisObj = loadJsonFixture(this.getClass(),
            "TEST-CA.analysis.multi-donor.json", Analysis.class);

    // expected result:
    val donor_1 = AnalysisCentricDonor.builder()
            .id("DO1")
            .gender("female")
            .submittedId("MDT-AP-0749")
            .specimen(buildSpecimenListForDonor1())
            .build();

    val donor_2 = AnalysisCentricDonor.builder()
            .id("DO2")
            .gender("female")
            .submittedId("MDT-AP-0749")
            .specimen(buildSpecimenListForDonor2())
            .build();

    val results = AnalysisCentricDocumentConverter.getDonors(analysisObj);

    assertNotNull(results);
    assertEquals(results.size(), 2);
    assertTrue(results.contains(donor_1));
    assertTrue(results.contains(donor_2));
  }

  private List<bio.overture.maestro.domain.entities.indexing.Specimen> buildSpecimenListForDonor1() {
    val specimen_1 = bio.overture.maestro.domain.entities.indexing.Specimen.builder()
            .id("SP1")
            .specimenTissueSource("Other")
            .submitterSpecimenId("MDT-AP-0749_tumor_specimen")
            .tumourNormalDesignation("Tumour")
            .specimenType("Primary tumour - solid tissue")
            .samples(bio.overture.maestro.domain.entities.indexing.Sample.builder()
                    .id("SA1")
                    .sampleType("DNA")
                    .submitterSampleId("MDT-AP-0749_tumor")
                    .matchedNormalSubmitterSampleId("PCSI_0216_St_R")
                    .build())
            .build();

    val specimen_2 = bio.overture.maestro.domain.entities.indexing.Specimen.builder()
            .id("SP2")
            .specimenTissueSource("Other")
            .submitterSpecimenId("MDT-AP-0749_tumor_specimen")
            .tumourNormalDesignation("Tumour")
            .specimenType("Primary tumour - solid tissue")
            .samples(bio.overture.maestro.domain.entities.indexing.Sample.builder()
                    .id("SA2")
                    .sampleType("DNA")
                    .submitterSampleId("MDT-AP-0749_tumor")
                    .matchedNormalSubmitterSampleId("PCSI_0216_St_R")
                    .build())
            .build();

    val list = new ArrayList<Specimen>();
    list.add(specimen_1);
    list.add(specimen_2);
    return list;
  }

  private List<bio.overture.maestro.domain.entities.indexing.Specimen> buildSpecimenListForDonor2() {
    val specimen_1 = bio.overture.maestro.domain.entities.indexing.Specimen.builder()
            .id("SP3")
            .specimenTissueSource("Other")
            .submitterSpecimenId("MDT-AP-0749_tumor_specimen")
            .tumourNormalDesignation("Tumour")
            .specimenType("Primary tumour - solid tissue")
            .samples(bio.overture.maestro.domain.entities.indexing.Sample.builder()
                    .id("SA3")
                    .sampleType("DNA")
                    .submitterSampleId("MDT-AP-0749_tumor")
                    .matchedNormalSubmitterSampleId("PCSI_0216_St_R")
                    .build())
            .build();

    val list = new ArrayList<Specimen>();
    list.add(specimen_1);
    return list;
  }
}
