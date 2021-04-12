package cq;

public class UnableToStartHttpServerException extends RuntimeException {
    public UnableToStartHttpServerException() {
    }

    public UnableToStartHttpServerException(String message) {
        super(message);
    }

    public UnableToStartHttpServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnableToStartHttpServerException(Throwable cause) {
        super(cause);
    }

    protected UnableToStartHttpServerException(String message, Throwable cause,
                                               boolean enableSuppression,
                                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
