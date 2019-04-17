package bio.overture.maestro.app.infra.adapter.outbound.notification;

import bio.overture.maestro.domain.api.NotificationChannel;
import bio.overture.maestro.domain.api.NotificationName;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Set;

/**
 * This channel will store any failure in an append only log file
 * it uses logback loggers to do the write operation instead of manually
 * writing to files.
 *
 * the logs go to separate log file. see logback-spring.xml for the configs.
 */
@Slf4j
@ConditionalOnProperty(value = "maestro.failure-log.enabled", havingValue = "true")
public class FileBasedFailuresLogger implements NotificationChannel {

    @Override
    public void send(IndexerNotification notification) {
        log.error("{}", notification);
    }

    @Override
    public Set<NotificationName> subscriptions() {
        return Set.of(
            NotificationName.STUDY_ANALYSES_FETCH_FAILED,
            NotificationName.FETCH_REPO_STUDIES_FAILED,
            NotificationName.CONVERT_ANALYSIS_TO_FILE_DOCS_FAILED,
            NotificationName.INDEX_REQ_FAILED
        );
    }
}
