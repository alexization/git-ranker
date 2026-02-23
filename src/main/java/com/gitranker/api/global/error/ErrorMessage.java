package com.gitranker.api.global.error;

import com.gitranker.api.global.i18n.MessageUtils;
import lombok.Getter;

@Getter
public class ErrorMessage {
    private final String type;
    private final String message;
    private final Object data;

    private ErrorMessage(String type, String message, Object data) {
        this.type = type;
        this.message = message;
        this.data = data;
    }

    public ErrorMessage(ErrorType errorType) {
        this(errorType, null);
    }

    public ErrorMessage(ErrorType errorType, Object data) {
        this(errorType.toString(), MessageUtils.getMessage(errorType.getMessageKey()), data);
    }

    public ErrorMessage(ErrorType errorType, String message, Object data) {
        this(errorType.toString(), message, data);
    }
}
