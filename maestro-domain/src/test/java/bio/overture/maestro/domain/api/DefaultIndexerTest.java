package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import bio.overture.maestro.domain.api.message.IndexStudyRepositoryCommand;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.entities.indexing.StorageType;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.maestro.domain.port.outbound.indexing.FileCentricIndexAdapter;
import bio.overture.maestro.domain.port.outbound.metadata.repository.StudyRepositoryDAO;
import bio.overture.maestro.domain.port.outbound.metadata.study.StudyDAO;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAllStudiesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetStudyAnalysesCommand;
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
import java.util.stream.Collectors;

import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static bio.overture.masestro.test.TestCategory.UNIT_TEST;
import static org.mockito.ArgumentMatchers.any;
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
    private StudyRepositoryDAO studyRepositoryDao;

    @Mock
    private StudyDAO studyDAO;

    @Mock
    private FileCentricIndexAdapter indexServerAdapter;

    private Indexer indexer;

    @BeforeEach
    void setUp() {
        reset(studyRepositoryDao, studyDAO, indexServerAdapter);
        this.indexer = new DefaultIndexer(indexServerAdapter, studyDAO, studyRepositoryDao);
    }

    @AfterEach
    void tearDown() {}

    @Test
    void indexStudyRepository() {

        //Given
        val repoCode = "TEST-REPO";
        val filesRepository = getStubFilesRepository();
        val studies = getExpectedStudies();

        val fileRepo = Mono.just(getStubFilesRepository());
        val result = IndexResult.builder().successful(true).build();
        val monoResult =  Mono.just(result);

        val getStudiesCmd = GetAllStudiesCommand.builder().filesRepositoryBaseUrl(filesRepository.getBaseUrl()).build();

        given(studyDAO.getStudies(eq(getStudiesCmd))).willReturn(Flux.fromIterable(studies));
        given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(fileRepo);

        for(Study study: studies) {
            val studyId = study.getStudyId();
            val command = GetStudyAnalysesCommand.builder()
                .filesRepositoryBaseUrl(filesRepository.getBaseUrl()).studyId(studyId)
                .build();
            val studyAnalyses = getStudyAnalyses(studyId);
            val fileCentricDocuments = getExpectedFileCentricDocument(studyId);
            val batchIndexFilesCommand = BatchIndexFilesCommand.builder().files(fileCentricDocuments).build();

            given(studyDAO.getStudyAnalyses(eq(command))).willReturn(Mono.just(studyAnalyses));
            given(indexServerAdapter.batchUpsertFileRepositories(eq(batchIndexFilesCommand))).willReturn(monoResult);
        }

        // When
        val indexResultMono = indexer.indexStudyRepository(IndexStudyRepositoryCommand.builder()
            .repositoryCode("TEST-REPO")
            .build());

        // Then
        StepVerifier.create(indexResultMono)
            .expectNext(result)
            .expectComplete()
            .verify();

        then(studyRepositoryDao).should(times(1)).getFilesRepository(repoCode);
        then(studyDAO).should(times(3)).getStudyAnalyses(any());
        then(indexServerAdapter).should(times(3)).batchUpsertFileRepositories(any());

    }


    @Test
    void indexStudy() {

        // Given
        val studyId = "PEME-CA";
        val repoCode = "TEST-REPO";
        val filesRepository = getStubFilesRepository();
        val a1 = getStudyAnalyses(studyId);
        val fileCentricDocuments = getExpectedFileCentricDocument(studyId);
        val fileRepo = Mono.just(getStubFilesRepository());
        val studyAnalyses = Mono.just(a1);
        val result = IndexResult.builder().successful(true).build();
        val monoResult =  Mono.just(result);
        val batchIndexFilesCommand = BatchIndexFilesCommand.builder().files(fileCentricDocuments).build();

        val getStudyAnalysesCommand = GetStudyAnalysesCommand.builder()
            .studyId(studyId)
            .filesRepositoryBaseUrl(filesRepository.getBaseUrl())
            .build();

        given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(fileRepo);
        given(studyDAO.getStudyAnalyses(eq(getStudyAnalysesCommand))).willReturn(studyAnalyses);
        given(indexServerAdapter.batchUpsertFileRepositories(eq(batchIndexFilesCommand))).willReturn(monoResult);

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

        then(studyRepositoryDao).should(times(1)).getFilesRepository(repoCode);
        then(studyDAO).should(times(1)).getStudyAnalyses(eq(getStudyAnalysesCommand));
        then(indexServerAdapter).should(times(1)).batchUpsertFileRepositories(eq(batchIndexFilesCommand));

    }

    @SneakyThrows
    private List<FileCentricDocument> getExpectedFileCentricDocument(String studyId) {
        return Arrays.asList(loadJsonFixture(getClass(), studyId + ".files.json", FileCentricDocument[].class));
    }

    private List<Study> getExpectedStudies() {
        return Arrays.stream(loadJsonFixture(getClass(), "studies.json", String[].class))
            .map(s -> Study.builder().studyId(s).build())
            .collect(Collectors.toList());
    }

    private List<Analysis> getStudyAnalyses(String studyId) {
        return Arrays.asList(loadJsonFixture(getClass(), studyId +".analysis.json", Analysis[].class));
    }

    private StudyRepository getStubFilesRepository() {
        return StudyRepository.builder()
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