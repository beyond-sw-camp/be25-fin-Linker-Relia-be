package com.linker.relia.infra.sms;

public class SmsSendException extends RuntimeException {

    public SmsSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
