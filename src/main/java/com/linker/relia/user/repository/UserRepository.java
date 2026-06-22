package com.linker.relia.user.repository;

import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = "organization")
    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    Optional<User> findByEmpCode(String empCode);

    @EntityGraph(attributePaths = "organization")
    List<User> findAllByDeletedAtIsNullOrderByEmpCodeAsc();

    @EntityGraph(attributePaths = "organization")
    Optional<User> findByOrganizationIdAndUserRoleAndDeletedAtIsNull(UUID organizationId, UserRole userRole);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "organization")
    @Query("select u from User u where u.id = :id and u.deletedAt is null")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);
}

