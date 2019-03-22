package bio.overture.maestro.app.infra.adapter.inbound.webapi;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

/**
 * This class is a generic representation for errors to be returned by the GlobalWebExceptionHandler
 */
@AllArgsConstructor
@Getter
class ErrorDetails {
    private Date timestamp;
    private String message;
    private String details;
}