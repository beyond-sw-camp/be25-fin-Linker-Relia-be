package com.linker.relia.user.service.empcode;

import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.user.domain.UserEmpCodeSequence;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.exception.UserErrorCode;
import com.linker.relia.user.repository.UserEmpCodeSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EmpCodeGeneratorImpl implements EmpCodeGenerator {
    private final EmpCodePolicy empCodePolicy;
    private final UserEmpCodeSequenceRepository userEmpCodeSequenceRepository;

    @Override
    @Transactional
    public String generate(UserRole userRole) {
        UserEmpCodeSequence sequence = userEmpCodeSequenceRepository.findByUserRoleForUpdate(userRole)
                .orElseThrow(() -> new BusinessException(UserErrorCode.EMP_CODE_SEQUENCE_NOT_FOUND));

        long issuedValue = sequence.issueNextValue(LocalDateTime.now());
        return empCodePolicy.getPrefix(userRole) + String.format("%03d", issuedValue);
    }
}
