package com.ticket.fast.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record ApiResponse<T>(
        boolean success,
        @JsonInclude(NON_NULL)
        T data,
        @JsonInclude(NON_NULL)
        ApiError error,
        LocalDateTime timestamp

) {
    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }
    public static <T> ApiResponse<T> error(String code, String message){
        return new ApiResponse<>(false, null, new ApiError(code, message), LocalDateTime.now());
    }
    public static <T> ApiResponse<T> error(String code, String message, T data){
        return new ApiResponse<>(false, data, new ApiError(code, message), LocalDateTime.now());
    }

    public record ApiError(
            String code,
            String message
    ) {
    }

}
