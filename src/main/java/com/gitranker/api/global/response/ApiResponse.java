package com.gitranker.api.global.response;

import com.gitranker.api.global.error.ErrorMessage;
import com.gitranker.api.global.error.ErrorType;
import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final ResultType result;
    private final T data;
    private final ErrorMessage error;

    private ApiResponse(ResultType result, T data, ErrorMessage error) {
        this.result = result;
        this.data = data;
        this.error = error;
    }

    public static ApiResponse<Object> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null);
    }

    public static <S> ApiResponse<S> success(S data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null);
    }

    public static <S> ApiResponse<S> error(ErrorType error) {
        return error(error, null);
    }

    public static <S> ApiResponse<S> error(ErrorType error, Object errorData) {
        return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(error, errorData));
    }

    public static <S> ApiResponse<S> error(ErrorType error, String message, Object errorData) {
        return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(error, message, errorData));
    }

}
