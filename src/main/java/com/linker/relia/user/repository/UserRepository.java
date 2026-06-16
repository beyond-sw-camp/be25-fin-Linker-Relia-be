package com.linker.relia.user.repository;

import com.linker.relia.user.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

