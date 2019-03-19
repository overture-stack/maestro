package bio.overture.maestro.infra.adapter.inbound.webapi;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@AllArgsConstructor
@Getter
class ErrorDetails {
    private Date timestamp;
    private String message;
    private String details;
}