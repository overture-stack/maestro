package bio.overture.maestro.app.infra.adapter.inbound.webapi;


import bio.overture.maestro.domain.api.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

import java.util.Date;

@Slf4j
@ControllerAdvice
public class GlobalWebExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Mono<ErrorDetails> resourceNotFoundException(NotFoundException ex, ServerHttpRequest request) {
        log.error("Resource not found exception", ex);
        return getErrorDetails(ex, request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Mono<ErrorDetails> globalExceptionHandler(Exception ex, ServerHttpRequest request) {
        log.error("Unhandled exception", ex);
        return getErrorDetails(ex, request);
    }

    private Mono<ErrorDetails> getErrorDetails(Exception ex, ServerHttpRequest request) {
        return Mono.just(new ErrorDetails(new Date(),
            ex.getMessage(), request.getPath().toString())
        );
    }

}