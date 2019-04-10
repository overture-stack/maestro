package bio.overture.maestro.domain.api.exception;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class FailureData {
    private List<String> ids;
    private String idType;
}
