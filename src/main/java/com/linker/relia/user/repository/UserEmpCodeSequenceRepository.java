package com.linker.relia.user.repository;

import com.linker.relia.user.domain.UserEmpCodeSequence;
import com.linker.relia.user.domain.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserEmpCodeSequenceRepository extends JpaRepository<UserEmpCodeSequence, UserRole> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserEmpCodeSequence s where s.userRole = :userRole")
    Optional<UserEmpCodeSequence> findByUserRoleForUpdate(@Param("userRole") UserRole userRole);
}
