package com.linker.relia.infra.sms;

public interface SmsClient {

    void send(String to, String text);
}
