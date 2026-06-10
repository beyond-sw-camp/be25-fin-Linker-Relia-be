package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationRenewalInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsultationRenewalInterestRepository extends JpaRepository<ConsultationRenewalInterest, UUID> {
}
