package bio.overture.maestro.infra.adapter.outbound;

import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.entities.studymetadata.Analysis;
import bio.overture.maestro.domain.port.outbound.StudyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.inject.Inject;

@Slf4j
public class SongStudyRepository implements StudyRepository {

    private final WebClient webClient;

    @Inject
    public SongStudyRepository(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Flux<Analysis> getStudyAnalyses(GetStudyAnalysesCommand getStudyAnalysesCommand) {
        return this.webClient.get()
                .uri(getStudyAnalysesCommand.getFilesRepositoryBaseUrl() + "/studies/" + getStudyAnalysesCommand.getStudyId() + "/analysis")
                .retrieve()
                .bodyToFlux(Analysis.class);
    }


}
