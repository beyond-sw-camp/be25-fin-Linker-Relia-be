package com.linker.relia.handover.event;

import com.linker.relia.infra.sms.SmsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandoverSmsEventListener {

    private final SmsClient smsClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendHandoverCompletedSms(HandoverSmsEvent event) {
        if (event.customerPhone() == null || event.customerPhone().isBlank()) {
            log.warn("인수인계 SMS 발송 생략. 고객 전화번호가 비어 있습니다.");
            return;
        }

        try {
            smsClient.send(
                    event.customerPhone(),
                    "고객님의 담당 설계사가 " + event.newFpName() + " 설계사로 변경되었습니다."
            );
        } catch (Exception e) {
            log.error("인수인계 SMS 발송 실패. 수신번호={}", maskPhoneNumber(event.customerPhone()), e);
        }
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "****";
        }

        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
