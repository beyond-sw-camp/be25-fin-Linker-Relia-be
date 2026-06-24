package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationClaimReviewItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultationClaimReviewItemRepository
        extends JpaRepository<ConsultationClaimReviewItem, UUID> {
    List<ConsultationClaimReviewItem> findAllByConsultationClaimDetailId(UUID claimDetailId);
}
