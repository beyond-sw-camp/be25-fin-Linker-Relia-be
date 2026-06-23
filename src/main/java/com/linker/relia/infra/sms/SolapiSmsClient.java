package com.linker.relia.infra.sms;

import com.linker.relia.infra.config.SolapiSmsProperties;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SolapiSmsClient implements SmsClient {

    private final SolapiSmsProperties properties;
    private final DefaultMessageService messageService;

    public SolapiSmsClient(SolapiSmsProperties properties) {
        this.properties = properties;
        this.messageService = SolapiClient.INSTANCE.createInstance(
                properties.getApiKey(),
                properties.getApiSecret()
        );
    }

    @Override
    public void send(String to, String text) {
        Message message = new Message();
        message.setFrom(properties.getFromNumber());
        message.setTo(to);
        message.setText(text);

        try {
            messageService.send(message, null);
            log.info("SMS 발송 성공. 수신번호={}", maskPhoneNumber(to));
        } catch (Exception e) {
            throw new SmsSendException("SMS 발송에 실패했습니다.", e);
        }
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "****";
        }

        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
