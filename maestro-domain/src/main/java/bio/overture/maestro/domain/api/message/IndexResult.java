package bio.overture.maestro.domain.api.message;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IndexResult {
    private boolean successful;
}
