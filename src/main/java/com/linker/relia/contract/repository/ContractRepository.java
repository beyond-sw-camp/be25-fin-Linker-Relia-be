package com.linker.relia.contract.repository;

import com.linker.relia.contract.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID>, ContractRepositoryCustom {
}
