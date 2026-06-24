package com.linker.relia.user.service.empcode;

import com.linker.relia.user.domain.UserRole;

public interface EmpCodeGenerator {
    String generate(UserRole userRole);
}
