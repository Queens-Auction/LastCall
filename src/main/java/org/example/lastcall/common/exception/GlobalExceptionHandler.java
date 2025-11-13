package org.example.lastcall.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.domain.auth.email.exception.EmailErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex) {
        ex.printStackTrace();
        ErrorCode errorCode = ex.getErrorCode();

        ApiResponse<?> errorResponse = ApiResponse.error(errorCode.getMessage());

        return new ResponseEntity<>(errorResponse, errorCode.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleSystemException(Exception ex) {
        String internalErrorMsg = "예상치 못한 서버 오류가 발생했습니다.";

        ApiResponse<?> errorResponse = ApiResponse.error(internalErrorMsg);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("잘못된 요청입니다.");

        ApiResponse<?> response = ApiResponse.error(message);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String rootCause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        String uri = request.getRequestURI();
        String message;

        if (rootCause.contains("UUID")) {
            message = EmailErrorCode.INVALID_UUID_FORMAT.getMessage();
        } else if (uri.contains("/login")) {
            message = "이메일 또는 비밀번호가 비어 있습니다.";
        } else if (uri.contains("/withdraw")) {
            return null;
        } else {
            message = "요청 본문이 비어 있거나 형식이 잘못되었습니다.";
        }

        ApiResponse<?> errorResponse = ApiResponse.error(message);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
