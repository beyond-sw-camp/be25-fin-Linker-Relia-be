package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationNewProposedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsultationNewProposedProductRepository extends JpaRepository<ConsultationNewProposedProduct, UUID> {
}
