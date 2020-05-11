/*
 *  Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.message.*;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.entities.indexing.Repository;
import bio.overture.maestro.domain.entities.indexing.StorageType;
import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import bio.overture.maestro.domain.entities.indexing.rules.IDExclusionRule;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Sample;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.maestro.domain.port.outbound.indexing.AnalysisCentricIndexAdapter;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.indexing.FileCentricIndexAdapter;
import bio.overture.maestro.domain.port.outbound.indexing.rules.ExclusionRulesDAO;
import bio.overture.maestro.domain.port.outbound.metadata.repository.StudyRepositoryDAO;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAllStudiesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAnalysisCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.StudyDAO;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import io.vavr.Tuple2;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.NotNull;
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
import java.util.*;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.api.DefaultIndexer.REPO_CODE;
import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static bio.overture.masestro.test.Fixture.loadJsonFixtureSnakeCase;
import static bio.overture.masestro.test.TestCategory.UNIT_TEST;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@Tag(UNIT_TEST)
class DefaultIndexerTest {

    @Mock
    private StudyRepositoryDAO studyRepositoryDao;

    @Mock
    private ExclusionRulesDAO exclusionRulesDAO;

    @Mock
    private StudyDAO studyDAO;

    @Mock
    private FileCentricIndexAdapter indexServerAdapter;

    @Mock
    private AnalysisCentricIndexAdapter analysisCentricIndexAdapter;

    @Mock
    private IndexEnabledProperties indexEnabledProperties;

    private DefaultIndexer indexer;

    @Mock
    private Notifier notifier;

    @BeforeEach
    void setUp() {
      reset(studyRepositoryDao, studyDAO, indexServerAdapter, analysisCentricIndexAdapter, notifier);
      given(indexEnabledProperties.isFileCentricEnabled()).willReturn(Boolean.TRUE);
      this.indexer = new DefaultIndexer(indexServerAdapter, analysisCentricIndexAdapter, studyDAO, studyRepositoryDao,
              exclusionRulesDAO, notifier, indexEnabledProperties);
    }

    @Test
    void indexStudyshouldNotifyOnStudyFetchError() {
        val repoCode = "TEST-REPO";
        val filesRepository = getStubFilesRepository();
        val repositoryMono = Mono.just(filesRepository);
        val failure = FailureData.builder()
            .failingIds(Map.of("studyId", Set.of("anyStudy")))
            .build();
        val output = IndexResult.builder()
            .failureData(failure)
            .successful(false)
            .build();

        given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(repositoryMono);
        given(studyDAO.getStudyAnalyses(any(GetStudyAnalysesCommand.class)))
            .willReturn(Mono.error(new IndexerException("failed", new RuntimeException(""), failure)));

        // When
        val indexResult = indexer.indexStudy(IndexStudyCommand.builder()
            .studyId("anyStudy")
            .repositoryCode(filesRepository.getCode())
            .build()
        );

        // Then
        StepVerifier.create(indexResult)
            .expectNext(output)
            .expectComplete()
            .verify();

        then(studyRepositoryDao).should(times(1)).getFilesRepository(repoCode);
        then(indexServerAdapter).should(times(0)).batchUpsertFileRepositories(any());
    }

    @Test
    void indexRepositoryShouldHandleFetchStudiesFailure() {
        val repoCode = "TEST-REPO";
        val filesRepository = getStubFilesRepository();
        val fileRepo = Mono.just(getStubFilesRepository());
        val failure = FailureData.builder()
            .failingIds(Map.of(REPO_CODE, Set.of(repoCode))).build();
        val failedIndexResult = IndexResult.builder().failureData(failure).successful(false).build();
        val getStudiesCmd = GetAllStudiesCommand.builder().filesRepositoryBaseUrl(filesRepository.getUrl()).build();

        given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(fileRepo);
        given(studyDAO.getStudies(eq(getStudiesCmd))).willReturn(Flux.error(new RuntimeException("sike!")));

        // When
        val indexResultMono = indexer.indexRepository(IndexStudyRepositoryCommand.builder()
            .repositoryCode("TEST-REPO")
            .build());

        // Then
        StepVerifier.create(indexResultMono)
            .expectNext(failedIndexResult)
            .expectComplete()
            .verify();

        then(studyRepositoryDao).should(times(1)).getFilesRepository(repoCode);
        then(studyDAO).should(times(0)).getStudyAnalyses(any());
        then(indexServerAdapter).should(times(0)).batchUpsertFileRepositories(any());
        then(notifier).should(times(1)).notify(any());
    }


    @Test
    void shouldExcludeSampleIdFromIndexing() {
        // Given
        val studyId = "PACA-CA";
        val repoCode = "TEST-REPO";
        val filesRepository = getStubFilesRepository();
        val a1 = getStudyAnalyses(studyId);
        // load the fixture with
        val fileCentricDocuments = Arrays.asList(loadJsonFixtureSnakeCase(getClass(),
            studyId + ".files.excluded.SA520221.json", FileCentricDocument[].class));
        val repositoryMono = Mono.just(filesRepository);
        val studyAnalyses = Mono.just(a1);
        val result = IndexResult.builder().successful(true).build();
        val monoResult =  Mono.just(result);
        val batchIndexFilesCommand = BatchIndexFilesCommand.builder().files(fileCentricDocuments).build();
        val getStudyAnalysesCommand = GetStudyAnalysesCommand.builder()
            .studyId(studyId)
            .filesRepositoryBaseUrl(filesRepository.getUrl())
            .build();

        Mono<Map<Class<?>, List<? extends ExclusionRule>>> sampleExclusionRule = Mono.just(
            Map.of(
                Sample.class, List.of(new IDExclusionRule(Sample.class, List.of("SA520221")))
            )
        );

        given(indexServerAdapter.fetchByIds(anyList())).willReturn(Mono.just(List.of()));
        given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(repositoryMono);
        given(studyDAO.getStudyAnalyses(eq(getStudyAnalysesCommand))).willReturn(studyAnalyses);
        given(indexServerAdapter.batchUpsertFileRepositories(eq(batchIndexFilesCommand))).willReturn(monoResult);
        given(exclusionRulesDAO.getExclusionRules()).willReturn(sampleExclusionRule);

        // When
        val indexResultFlux = indexer.indexStudy(IndexStudyCommand.builder()
            .studyId(studyId)
            .repositoryCode(filesRepository.getCode())
            .build());

        // Then
        StepVerifier.create(indexResultFlux)
            .expectNext(result)
            .expectComplete()
            .verify();

        then(studyRepositoryDao).should(times(1)).getFilesRepository(repoCode);
    }

  @Test
  void shouldDetectConflictInFileAndDeleteItFromIndex() {
    // Given
    val studyId = "MALY-DE";
    val repoCode = "TEST-REPO";
    val filesRepository = getStubFilesRepository();
    // Note: in MALE-DE.conflicting.analysis.json, conflict is: analysis 0a1df2a2-029d-48cc-839c-0a7c89ff972f is UNPUBLISHED
    val a1 = Arrays.asList(loadJsonFixture(getClass(), studyId +".conflicting.analysis.json", Analysis[].class));
    val fileCentricDocuments = getExpectedFileCentricDocument(studyId);
    val nonConflictingDocs = fileCentricDocuments.subList(1, fileCentricDocuments.size());
    val fileRepo = Mono.just(getStubFilesRepository());
    val studyAnalyses = Mono.just(a1);

    val result = IndexResult.builder().successful(true).build();
    val monoResult =  Mono.just(result);
    val batchIndexFilesCommand = BatchIndexFilesCommand.builder().files(nonConflictingDocs).build();
    val getStudyAnalysesCommand = GetStudyAnalysesCommand.builder()
        .studyId(studyId)
        .filesRepositoryBaseUrl(filesRepository.getUrl())
        .build();
    val expectedNotification = new IndexerNotification(NotificationName.INDEX_FILE_CONFLICT,
        getConflicts(fileCentricDocuments));

    given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(fileRepo);
    given(studyDAO.getStudyAnalyses(eq(getStudyAnalysesCommand))).willReturn(studyAnalyses);
    given(indexServerAdapter.fetchByIds(anyList())).willReturn(Mono.just(List.of(fileCentricDocuments.get(0))));
    given(indexServerAdapter.batchUpsertFileRepositories(eq(batchIndexFilesCommand))).willReturn(monoResult);
    given(exclusionRulesDAO.getExclusionRules()).willReturn(Mono.just(Map.of()));

    // When
    val command = IndexStudyCommand.builder()
        .studyId(studyId)
        .repositoryCode(filesRepository.getCode())
        .build();

    val indexResultMono = indexer.indexStudy(command);

    // Then
    StepVerifier.create(indexResultMono)
        .expectNext(result)
        .expectComplete()
        .verify();

    then(notifier).should(times(1)).notify(eq(expectedNotification));
    then(indexServerAdapter).should(times(1)).batchUpsertFileRepositories(eq(batchIndexFilesCommand));
    then(indexServerAdapter).should(times(0)).removeFiles(eq(Set.of(fileCentricDocuments.get(0).getObjectId())));
  }

    @NotNull
    private Map<String, Object> getConflicts(List<FileCentricDocument> fileCentricDocuments) {
        return Map.of("conflicts", List.of(DefaultIndexer.FileConflict.builder()
            .indexedFile(
                DefaultIndexer.ConflictingFile.builder()
                    .studyId(fileCentricDocuments.get(0).getStudyId())
                    .analysisId(fileCentricDocuments.get(0).getAnalysis().getAnalysisId())
                    .objectId(fileCentricDocuments.get(0).getObjectId())
                    .repoCode(fileCentricDocuments.get(0).getRepositories().stream().map(Repository::getCode)
                        .collect(Collectors.toUnmodifiableSet()))
                    .build()
            ).newFile(
                DefaultIndexer.ConflictingFile.builder()
                    .studyId(fileCentricDocuments.get(0).getStudyId())
                    .analysisId(fileCentricDocuments.get(0).getAnalysis().getAnalysisId())
                    .objectId(fileCentricDocuments.get(0).getObjectId())
                    .repoCode(fileCentricDocuments.get(0).getRepositories().stream().map(Repository::getCode)
                        .collect(Collectors.toUnmodifiableSet()))
                    .build()
            ).build()));
    }

    @Test
    void shouldIndexSingleAnalysis() {
        // Given
        val studyId = "PEME-CA";
        val repoCode = "TEST-REPO";
        val analysisId = "EGAZ00001254368";
        val filesRepository = getStubFilesRepository();
        val a1 = loadJsonFixture(getClass(), studyId +".analysis.EGAZ00001254368.json", Analysis.class);
        val fileCentricDocuments = getExpectedFileCentricDocument(studyId);
        val fileRepo = Mono.just(getStubFilesRepository());
        val studyAnalysis = Mono.just(a1);
        val result = IndexResult.builder().successful(true).build();
        val monoResult =  Mono.just(result);
        val batchIndexFilesCommand = BatchIndexFilesCommand.builder().files(fileCentricDocuments).build();
        val getStudyAnalysesCommand = GetAnalysisCommand.builder()
            .studyId(studyId)
            .analysisId(analysisId)
            .filesRepositoryBaseUrl(filesRepository.getUrl())
            .build();

        given(indexServerAdapter.batchUpsertFileRepositories(eq(batchIndexFilesCommand))).willReturn(monoResult);
        given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(fileRepo);
        given(studyDAO.getAnalysis(eq(getStudyAnalysesCommand))).willReturn(studyAnalysis);
        given(exclusionRulesDAO.getExclusionRules()).willReturn(Mono.just(Map.of()));
        given(indexServerAdapter.fetchByIds(anyList())).willReturn(Mono.just(List.of()));

        // When
        val indexResultMono = indexer.indexAnalysisToFileCentric(IndexAnalysisCommand.builder()
            .analysisIdentifier(AnalysisIdentifier.builder()
                .studyId(studyId)
                .analysisId(analysisId)
                .repositoryCode(filesRepository.getCode())
                .build()
            ).build()
        );

        // Then
        StepVerifier.create(indexResultMono)
            .expectNext(result)
            .expectComplete()
            .verify();

        then(studyRepositoryDao).should(times(1)).getFilesRepository(repoCode);
        then(studyDAO).should(times(1)).getAnalysis(eq(getStudyAnalysesCommand));
        then(indexServerAdapter).should(times(1)).batchUpsertFileRepositories(eq(batchIndexFilesCommand));
    }

    @Test
    void shouldRemoveSingleAnalysis() {
        // Given
        val studyId = "PEME-CA";
        val analysisId = "EGAZ00001254368";
        val filesRepository = getStubFilesRepository();
        val result = IndexResult.builder().successful(true).build();
        val monoResult =  Mono.<Void>fromSupplier(() -> null);

        given(indexServerAdapter.removeAnalysisFiles(eq(analysisId))).willReturn(monoResult);

        // When
        val indexResultMono = indexer.removeAnalysis(RemoveAnalysisCommand.builder()
            .analysisIdentifier(AnalysisIdentifier.builder()
                .studyId(studyId)
                .analysisId(analysisId)
                .repositoryCode(filesRepository.getCode())
                .build()
            ).build()
        );

        // Then
        StepVerifier.create(indexResultMono)
            .expectNext(result)
            .expectComplete()
            .verify();

        then(indexServerAdapter).should(times(1)).removeAnalysisFiles(eq(analysisId));

    }

    @Test
    void shouldNotIndexAnalysisIfExcludedByRule() {
        // Given
        val studyId = "PEME-CA";
        val repoCode = "TEST-REPO";
        val analysisId = "EGAZ00001254368";
        val filesRepository = getStubFilesRepository();
        val a1 = loadJsonFixture(getClass(), studyId +".analysis.EGAZ00001254368.json", Analysis.class);
        val fileCentricDocuments = List.<FileCentricDocument>of();
        val fileRepoMono = Mono.just(getStubFilesRepository());
        val studyAnalysis = Mono.just(a1);
        val result = IndexResult.builder().successful(true).build();
        val monoResult =  Mono.just(result);
        val batchIndexFilesCommand = BatchIndexFilesCommand.builder().files(fileCentricDocuments).build();
        val getStudyAnalysesCommand = GetAnalysisCommand.builder()
            .studyId(studyId)
            .analysisId(analysisId)
            .filesRepositoryBaseUrl(filesRepository.getUrl())
            .build();
        val sampleExclusionRule = Mono.<Map<Class<?>, List<? extends ExclusionRule>>>just(
            Map.of(
                Analysis.class, List.of(new IDExclusionRule(Analysis.class, List.of("EGAZ00001254368")))
            )
        );

        given(indexServerAdapter.fetchByIds(anyList())).willReturn(Mono.just(List.of()));
        given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(fileRepoMono);
        given(studyDAO.getAnalysis(eq(getStudyAnalysesCommand))).willReturn(studyAnalysis);
        given(indexServerAdapter.batchUpsertFileRepositories(eq(batchIndexFilesCommand))).willReturn(monoResult);
        given(exclusionRulesDAO.getExclusionRules()).willReturn(sampleExclusionRule);

        // When
        val indexResultMono = indexer.indexAnalysisToFileCentric(IndexAnalysisCommand.builder()
            .analysisIdentifier(AnalysisIdentifier.builder()
                .studyId(studyId)
                .analysisId(analysisId)
                .repositoryCode(filesRepository.getCode())
                .build()
            ).build()
        );

        // Then
        StepVerifier.create(indexResultMono)
            .expectNext(result)
            .expectComplete()
            .verify();

        then(studyRepositoryDao).should(times(1)).getFilesRepository(repoCode);
        then(studyDAO).should(times(1)).getAnalysis(eq(getStudyAnalysesCommand));
        then(indexServerAdapter).should(times(1))
            .batchUpsertFileRepositories(eq(batchIndexFilesCommand));

    }

    @Test
    void indexRepositoryshouldNotifyOnStudyFetchError() {
        //Given
        val repoCode = "TEST-REPO";
        val filesRepository = getStubFilesRepository();
        val fileRepo = Mono.just(getStubFilesRepository());
        val failure = FailureData.builder()
            .failingIds(Map.of("studyId", Set.of("PACA-CA"))).build();
        val failedIndexResult = IndexResult.builder().failureData(failure).successful(false).build();
        val getStudiesCmd = GetAllStudiesCommand.builder().filesRepositoryBaseUrl(filesRepository.getUrl()).build();

        given(studyDAO.getStudies(eq(getStudiesCmd))).willReturn(Flux.error(new IndexerException("failed", new RuntimeException(""), failure)));
        given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(fileRepo);

        // When
        val indexResultMono = indexer.indexRepository(IndexStudyRepositoryCommand.builder()
            .repositoryCode("TEST-REPO")
            .build());

        // Then
        StepVerifier.create(indexResultMono)
            .expectNext(failedIndexResult)
            .expectComplete()
            .verify();

        then(studyRepositoryDao).should(times(1)).getFilesRepository(repoCode);
        then(notifier).should(times(1)).notify(any());
    }

    @Test
    void shouldIndexAllRepositoryStudies() {
      //Given
      val repoCode = "TEST-REPO";
      val filesRepository = getStubFilesRepository();
      val studies = getExpectedStudies();
      val fileRepo = Mono.just(getStubFilesRepository());
      val result = IndexResult.builder().successful(true).build();
      val monoResult =  Mono.just(result);
      val getStudiesCmd = GetAllStudiesCommand.builder().filesRepositoryBaseUrl(filesRepository.getUrl()).build();

      given(indexServerAdapter.fetchByIds(anyList())).willReturn(Mono.just(List.of()));
      given(studyDAO.getStudies(eq(getStudiesCmd))).willReturn(Flux.fromIterable(studies));
      given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(fileRepo);
      given(exclusionRulesDAO.getExclusionRules()).willReturn(Mono.just(Map.of()));

      for(Study study: studies) {
        val studyId = study.getStudyId();
        val command = GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl(filesRepository.getUrl()).studyId(studyId)
            .build();
        val studyAnalyses = getStudyAnalyses(studyId);
        val fileCentricDocuments = getExpectedFileCentricDocument(studyId);
        val batchIndexFilesCommand = BatchIndexFilesCommand.builder().files(fileCentricDocuments).build();

        given(studyDAO.getStudyAnalyses(eq(command))).willReturn(Mono.just(studyAnalyses));
        given(indexServerAdapter.batchUpsertFileRepositories(eq(batchIndexFilesCommand))).willReturn(monoResult);
      }

      // When
      val indexResultMono = indexer.indexRepository(IndexStudyRepositoryCommand.builder()
          .repositoryCode("TEST-REPO")
          .build());

      // Then
      StepVerifier.create(indexResultMono)
          .expectNext(result)
          .expectComplete()
          .verify();

      then(studyRepositoryDao).should(times(4)).getFilesRepository(repoCode);
      then(studyDAO).should(times(3)).getStudyAnalyses(any());
      then(indexServerAdapter).should(times(3)).batchUpsertFileRepositories(any());
    }

    @Test
    void shouldIndexSingleStudy() {
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
          .filesRepositoryBaseUrl(filesRepository.getUrl())
          .build();

      given(indexServerAdapter.fetchByIds(anyList())).willReturn(Mono.just(List.of()));
      given(studyRepositoryDao.getFilesRepository(eq(repoCode))).willReturn(fileRepo);
      given(studyDAO.getStudyAnalyses(eq(getStudyAnalysesCommand))).willReturn(studyAnalyses);
      given(indexServerAdapter.batchUpsertFileRepositories(eq(batchIndexFilesCommand))).willReturn(monoResult);
      given(exclusionRulesDAO.getExclusionRules()).willReturn(Mono.just(Map.of()));

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
        return Arrays.asList(loadJsonFixtureSnakeCase(getClass(), studyId + ".files.json", FileCentricDocument[].class));
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
            .url("http://song.sing.sung")
            .code("TEST-REPO")
            .country("CA")
            .dataPath("/p1/p2")
            .organization("org")
            .storageType(StorageType.S3)
            .metadataPath("/m1/m2")
            .build();
    }

}