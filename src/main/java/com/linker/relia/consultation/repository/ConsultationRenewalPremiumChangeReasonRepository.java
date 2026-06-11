package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationRenewalPremiumChangeReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultationRenewalPremiumChangeReasonRepository
        extends JpaRepository<ConsultationRenewalPremiumChangeReason, UUID> {
    List<ConsultationRenewalPremiumChangeReason>
    findAllByConsultationRenewalDetailId(UUID renewalDetailId);
}