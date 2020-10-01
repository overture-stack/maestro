package bio.overture.maestro.domain.api;

import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static bio.overture.masestro.test.TestCategory.UNIT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import bio.overture.maestro.domain.api.message.ConvertAnalysisCommand;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.entities.indexing.StorageType;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.port.outbound.metadata.repository.StudyRepositoryDAO;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@Tag(UNIT_TEST)
public class ConverterTest {
  @Mock private StudyRepositoryDAO studyRepositoryDao;

  private Converter converter;

  @BeforeEach
  public void before() {
    converter = new Converter(studyRepositoryDao);
  }

  @Test
  void convertAnalysesShouldReturnListOfFileDocuments() {
    val repoCode = "TEST-REPO";
    val fileRepo = Mono.just(getStubFilesRepository());
    given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(fileRepo);
    val analysisObj = loadJsonFixture(getClass(), "LIRI-JP.analysis.json", Analysis[].class);
    val expectedConversionResult =
        loadJsonFixture(
            getClass(),
            "LIRI-JP.converted.json",
            new TypeReference<Map<String, List<FileCentricDocument>>>() {});
    val result =
        converter.convertAnalysesToFileDocuments(
            ConvertAnalysisCommand.builder()
                .analyses(Arrays.asList(analysisObj))
                .repoCode(repoCode)
                .build());
    val docs = result.block();
    assertEquals(expectedConversionResult, docs);
  }

  private StudyRepository getStubFilesRepository() {
    return StudyRepository.builder()
        .name("singer")
        .url("http://song.sing.sung")
        .code("TEST-REPO")
        .country("CA")
        .organization("org")
        .storageType(StorageType.S3)
        .build();
  }
}
