package com.linker.relia.user.service.empcode;

import com.linker.relia.user.domain.UserRole;
import org.springframework.stereotype.Component;

@Component
public class EmpCodePolicy {
    public String getPrefix(UserRole userRole) {
        return switch (userRole) {
            case FP -> "FP";
            case BRANCH_MANAGER -> "BM";
            case HQ_MANAGER -> "HQ";
            case SYSTEM_ADMIN -> "SYS";
        };
    }
}
