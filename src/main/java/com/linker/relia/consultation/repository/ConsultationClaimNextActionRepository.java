package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationClaimNextAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultationClaimNextActionRepository
        extends JpaRepository<ConsultationClaimNextAction, UUID> {

    List<ConsultationClaimNextAction>
    findAllByConsultationClaimDetailIdOrderByActionOrderAsc(UUID claimDetailId);
}
