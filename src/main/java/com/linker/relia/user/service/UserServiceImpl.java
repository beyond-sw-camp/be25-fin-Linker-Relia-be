package com.linker.relia.user.service;

import com.linker.relia.common.audit.AuditActorIds;
import com.linker.relia.common.audit.AuditContextHolder;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.domain.OrganizationStatus;
import com.linker.relia.organization.domain.OrganizationType;
import com.linker.relia.organization.exception.OrganizationErrorCode;
import com.linker.relia.organization.repository.OrganizationRepository;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.domain.UserStatus;
import com.linker.relia.user.dto.FpSignupRequest;
import com.linker.relia.user.dto.FpSignupResponse;
import com.linker.relia.user.exception.UserErrorCode;
import com.linker.relia.user.repository.UserRepository;
import com.linker.relia.user.service.empcode.EmpCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmpCodeGenerator empCodeGenerator;

    @Override
    @Transactional
    public FpSignupResponse createFpUser(FpSignupRequest request) {
        String loginId = request.getLoginId().trim();
        String email = request.getEmail().trim();
        String organizationCode = request.getOrganizationCode().trim();

        if (userRepository.existsByLoginId(loginId)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        Organization organization = organizationRepository.findByOrganizationCode(organizationCode)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

        // 조직의 타입이 지점이 아니거나 비활성화 상태일 경우 예외 처리
        if (organization.getOrganizationType() != OrganizationType.BRANCH
                || organization.getOrganizationStatus() != OrganizationStatus.ACTIVE) {
            throw new BusinessException(OrganizationErrorCode.INVALID_BRANCH_ORGANIZATION);
        }

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .empCode(empCodeGenerator.generate(UserRole.FP))
                .loginId(loginId)
                .password(passwordEncoder.encode(request.getPassword()))
                .userName(request.getUserName().trim())
                .userRole(UserRole.FP)
                .organization(organization)
                .userStatus(UserStatus.ACTIVE)
                .phone(normalizeNullable(request.getPhone()))
                .email(email)
                .joinedAt(LocalDate.now())
                .build();

        User savedUser;
        AuditContextHolder.setCurrentAuditor(AuditActorIds.PUBLIC_SIGNUP_USER_ID);
        try {
            savedUser = userRepository.save(user);
        } finally {
            AuditContextHolder.clear();
        }

        return FpSignupResponse.builder()
                .userId(savedUser.getId().toString())
                .empCode(savedUser.getEmpCode())
                .loginId(savedUser.getLoginId())
                .userName(savedUser.getUserName())
                .email(savedUser.getEmail())
                .organizationCode(organization.getOrganizationCode())
                .build();
    }

    // null, empty, blank 입력을 null로 정규화한다.
    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
