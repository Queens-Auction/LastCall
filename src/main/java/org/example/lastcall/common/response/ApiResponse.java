package org.example.lastcall.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.Instant;

@Schema(description = "API 공통 응답 포맷")
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @Schema(description = "요청 성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
    private final String message;

    @Schema(description = "응답 데이터 (null일 수 있음)")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final T data;

    @Schema(description = "응답 생성 시각 (UTC)", example = "2025-10-24T12:34:56Z")
    private final Instant timestamp;

    public ApiResponse(boolean success,
                       String message,
                       T data
    ) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}