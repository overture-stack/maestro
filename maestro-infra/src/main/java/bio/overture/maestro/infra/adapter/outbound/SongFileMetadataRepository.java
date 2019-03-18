package bio.overture.maestro.infra.adapter.outbound;

import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.port.outbound.FileMetadataRepository;
import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.inject.Inject;

import static bio.overture.maestro.domain.utility.Exceptions.notFound;
import static java.text.MessageFormat.format;
import static reactor.core.publisher.Mono.error;

@Slf4j
public class SongFileMetadataRepository implements FileMetadataRepository {

    private static final String STUDY_ANALYSES_URL_TEMPLATE = "{0}/studies/{1}/analysis";
    private static final String MSG_STUDY_DOES_NOT_EXIST = "study {0} doesn't exist in the specified repository";
    private final WebClient webClient;

    @Inject
    public SongFileMetadataRepository(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Flux<Analysis> getStudyAnalyses(GetStudyAnalysesCommand getStudyAnalysesCommand) {
        log.trace("in getStudyAnalyses, args: {} ", getStudyAnalysesCommand);
        val repoBaseUrl = getStudyAnalysesCommand.getFilesRepositoryBaseUrl();
        val studyId = getStudyAnalysesCommand.getStudyId();
        return this.webClient.get()
                .uri(format(STUDY_ANALYSES_URL_TEMPLATE, repoBaseUrl, studyId))
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                    clientResponse -> error(notFound(MSG_STUDY_DOES_NOT_EXIST, studyId)))
                .bodyToFlux(Analysis.class);
    }

}
