package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationClaimReviewItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsultationClaimReviewItemRepository extends JpaRepository<ConsultationClaimReviewItem, UUID> {
}
