package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationNewProposedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultationNewProposedProductRepository
        extends JpaRepository<ConsultationNewProposedProduct, UUID> {

    List<ConsultationNewProposedProduct>
    findAllByConsultationNewDetailId(UUID consultationNewDetailId);
}
