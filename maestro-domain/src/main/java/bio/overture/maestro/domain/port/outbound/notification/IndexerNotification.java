package bio.overture.maestro.domain.port.outbound.notification;


import bio.overture.maestro.domain.api.NotificationName;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

import static java.text.MessageFormat.format;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class IndexerNotification {
    private final NotificationName notificationName;
    private final Map<String, Object> attributes;

    public String toString() {
        return format("{0} | {1}", notificationName.name().toUpperCase(), attributes);
    }
}
