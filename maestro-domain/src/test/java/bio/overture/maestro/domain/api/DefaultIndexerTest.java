package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import bio.overture.maestro.domain.entities.indexer.FileMetadataRepository;
import bio.overture.maestro.domain.entities.indexer.StorageType;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.port.outbound.FileDocumentIndexingAdapter;
import bio.overture.maestro.domain.port.outbound.FileMetadataRepositoryStore;
import bio.overture.maestro.domain.port.outbound.message.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static bio.overture.masestro.test.TestCategory.UNIT_TEST;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@Tag(UNIT_TEST)
class DefaultIndexerTest {

    @Mock
    private FileMetadataRepositoryStore fileMetadataRepositoryStore;

    @Mock
    private bio.overture.maestro.domain.port.outbound.FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileDocumentIndexingAdapter indexServerAdapter;

    private Indexer indexer;

    @BeforeEach
    void setUp() {
        reset(fileMetadataRepositoryStore, fileMetadataRepository, indexServerAdapter);
        this.indexer = new DefaultIndexer(indexServerAdapter, fileMetadataRepository, fileMetadataRepositoryStore);
    }

    @AfterEach
    void tearDown() {}

    @Test
    void indexAll() {}

    @Test
    void indexStudy() {
        // Given
        val studyId = "PEME-CA";
        val repoCode = "TEST-REPO";
        val filesRepository = getStubFilesRepository();
        val a1 = getAnalysis();
        val fileCentricDocuments = getExpectedFileCentricDocument();
        val fileRepo = Mono.just(getStubFilesRepository());
        val studyAnalyses = Flux.just(a1);
        val result = IndexResult.builder().successful(true).build();
        val monoResult =  Mono.just(result);
        val batchIndexFilesCommand = BatchIndexFilesCommand.builder().files(fileCentricDocuments).build();
        val getStudyAnalysesCommand = GetStudyAnalysesCommand.builder()
            .studyId(studyId)
            .filesRepositoryBaseUrl(filesRepository.getBaseUrl())
            .build();

        given(fileMetadataRepositoryStore.getFilesRepository(eq(repoCode))).willReturn(fileRepo);
        given(fileMetadataRepository.getStudyAnalyses(eq(getStudyAnalysesCommand))).willReturn(studyAnalyses);
        given(indexServerAdapter.batchIndexFiles(eq(batchIndexFilesCommand))).willReturn(monoResult);

        // When
        val indexResultMono = indexer.indexStudy(IndexStudyCommand.builder()
            .studyId(studyId)
            .repositoryCode(filesRepository.getCode())
            .build()
        );

        // Then
        StepVerifier.create(indexResultMono)
            .expectNext(result)
            .expectComplete()
            .verify();

        then(fileMetadataRepositoryStore).should(times(1)).getFilesRepository(repoCode);
        then(fileMetadataRepository).should(times(1)).getStudyAnalyses(eq(getStudyAnalysesCommand));
        then(indexServerAdapter).should(times(1)).batchIndexFiles(eq(batchIndexFilesCommand));
    }

    @SneakyThrows
    private List<FileCentricDocument> getExpectedFileCentricDocument() {
        return Arrays.asList(loadJsonFixture(getClass(), "filecentricDocument.json", FileCentricDocument[].class));
    }

    @SneakyThrows
    private Analysis getAnalysis() {
        return loadJsonFixture(getClass(), "analysis.json", Analysis.class);
    }

    private FileMetadataRepository getStubFilesRepository() {
        return FileMetadataRepository.builder()
            .name("singer")
            .baseUrl("http://song.sing.sung")
            .code("TEST-REPO")
            .country("CA")
            .dataPath("/p1/p2")
            .organization("org")
            .storageType(StorageType.S3)
            .metadataPath("/m1/m2")
            .build();
    }

}