package com.srijan.portfolio.util;

import com.srijan.portfolio.dto.ApiErrorResponse;
import com.srijan.portfolio.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(success(data, message));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(success(data, message));
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static ApiErrorResponse error(String code, String message) {
        return ApiErrorResponse.builder()
                .success(false)
                .error(ApiErrorResponse.ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }
}
