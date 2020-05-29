package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDonor;
import bio.overture.maestro.domain.entities.metadata.study.*;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static bio.overture.maestro.domain.api.EntityGenerator.*;
import static bio.overture.masestro.test.Fixture.loadConverterTestFixture;
import static bio.overture.masestro.test.TestCategory.UNIT_TEST;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@Tag(UNIT_TEST)
public class AnalysisCentricDocumentConverterTest {

  @Test
  void testGetDonor() {
    val analysisObj = loadConverterTestFixture("TEST-CA.analysis.json", Analysis.class);

    // expected:
    val donor = AnalysisCentricDonor.builder()
        .donorId("DO1")
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
    val analysisObj = loadConverterTestFixture("TEST-CA.analysis.multi-donor.json", Analysis.class);

    // expected results:
    val donor_1 = AnalysisCentricDonor.builder()
            .donorId("DO1")
            .gender("Female")
            .submitterDonorId("MDT-AP-0749")
            .specimens(buildSpecimenListForDonor1())
            .build();

    val donor_2 = AnalysisCentricDonor.builder()
            .donorId("DO2")
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
}
