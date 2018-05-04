package com.github.rosclient;

public class RosException extends RuntimeException {

    public RosException() {
        super();
    }

    public RosException(String message) {
        super(message);
    }

    public RosException(String message, Throwable cause) {
        super(message, cause);
    }
}
