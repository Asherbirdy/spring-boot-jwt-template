package com.app.security.dto;

import com.app.security.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class Response<T> extends ResponseEntity<ApiResponse<T>> {

    public Response(String msg, T data, HttpStatus status) {
        super(new ApiResponse<>(msg, data), status);
    }
}
