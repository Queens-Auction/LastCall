package org.example.lastcall.common.exception;

import org.example.lastcall.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex) {
        ex.printStackTrace();
        ErrorCode errorCode = ex.getErrorCode();

        ApiResponse<?> errorResponse = ApiResponse.error(errorCode.getMessage());

        return new ResponseEntity<>(errorResponse, errorCode.getStatus());
    }

    @ExceptionHandler(InsufficientAuthenticationException.class) //콘솔창 로그
    public ResponseEntity<ApiResponse<?>> handleInsufficientAuthenticationException(Exception ex) {
        String internalErrorMsg = "로그인한 사용자만 이용할 수 있습니다.";

        ApiResponse<?> errorResponse = ApiResponse.error(internalErrorMsg);

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN); // 403
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleSystemException(Exception ex) {
        String internalErrorMsg = "예상치 못한 서버 오류가 발생했습니다.";

        ApiResponse<?> errorResponse = ApiResponse.error(internalErrorMsg);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing // 같은 필드 중복 메시지는 하나만
                ));
        String detailedMessage = errors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));

        ApiResponse<Map<String, String>> response = ApiResponse.error(detailedMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
