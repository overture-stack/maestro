package bio.overture.maestro.app.infra.adapter.outbound.notification;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    LoggingNotificationChannel.class
})
public class NotificationConfig {}
