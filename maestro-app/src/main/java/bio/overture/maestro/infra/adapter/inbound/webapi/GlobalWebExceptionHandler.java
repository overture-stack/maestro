package bio.overture.maestro.infra.adapter.inbound.webapi;


import bio.overture.maestro.domain.api.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Date;

@ControllerAdvice
@Slf4j
public class GlobalWebExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorDetails resourceNotFoundException(NotFoundException ex, ServerHttpRequest request) {
        log.error("resource not found exception", ex);
        return getErrorDetails(ex, request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorDetails globalExcpetionHandler(Exception ex, ServerHttpRequest request) {
        log.error("unhandled exception", ex);
        return getErrorDetails(ex, request);
    }

    private ErrorDetails getErrorDetails(Exception ex, ServerHttpRequest request) {
        return new ErrorDetails(new Date(), ex.getMessage(), request.getPath().toString());
    }
}