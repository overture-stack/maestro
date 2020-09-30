package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.message.ConvertAnalysisCommand;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.port.outbound.metadata.repository.StudyRepositoryDAO;
import io.vavr.Tuple2;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Converter {
  private final StudyRepositoryDAO studyRepositoryDao;

  @Inject
  public Converter(StudyRepositoryDAO studyRepositoryDao) {
    this.studyRepositoryDao = studyRepositoryDao;
  }

  public Mono<Map<String, List<FileCentricDocument>>> convertAnalysesToFileDocuments(
      @NonNull ConvertAnalysisCommand command) {
    return this.studyRepositoryDao
        .getFilesRepository(command.getRepoCode())
        .flatMapMany(
            studyRepository ->
                Flux.just(command.getAnalyses().toArray(new Analysis[0]))
                    .map(analysis -> new Tuple2<>(studyRepository, analysis)))
        .map(
            t ->
                new Tuple2<>(
                    t._2().getAnalysisId(),
                    FileCentricDocumentConverter.fromAnalysis(t._2(), t._1())))
        .reduce(
            new HashMap<>(),
            (acc, newTuple) -> {
              acc.put(newTuple._1(), newTuple._2());
              return acc;
            });
  }
}
