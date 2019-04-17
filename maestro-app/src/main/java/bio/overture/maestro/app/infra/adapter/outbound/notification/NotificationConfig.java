package bio.overture.maestro.app.infra.adapter.outbound.notification;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    LoggingNotificationChannel.class,
    FileBasedFailuresLogger.class,
})
public class NotificationConfig {}
