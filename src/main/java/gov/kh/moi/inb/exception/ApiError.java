package gov.kh.moi.inb.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private boolean success;
    private int status;
    private String error;
    private String message;
    private String path;
    private OffsetDateTime timestamp;
    private List<FieldViolation> violations;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldViolation {
        private String field;
        private String message;
    }
}
