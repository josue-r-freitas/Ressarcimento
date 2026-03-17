package br.com.empresa.ressarcimento.shared.api;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponse {

    OffsetDateTime timestamp;
    int status;
    String error;
    String message;
    String path;
    List<FieldError> fieldErrors;

    @Value
    @Builder
    public static class FieldError {
        String field;
        String rejectedValue;
        String message;
        Integer lineNumber;
    }
}

