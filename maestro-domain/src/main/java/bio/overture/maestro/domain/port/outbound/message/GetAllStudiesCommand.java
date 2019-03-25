package bio.overture.maestro.domain.port.outbound.message;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class GetAllStudiesCommand {
    @NonNull
    private String filesRepositoryBaseUrl;
}
