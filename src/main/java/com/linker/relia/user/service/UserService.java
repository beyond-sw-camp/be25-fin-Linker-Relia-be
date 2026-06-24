package com.linker.relia.user.service;

import com.linker.relia.user.dto.FpSignupRequest;
import com.linker.relia.user.dto.FpSignupResponse;

public interface UserService {
    FpSignupResponse createFpUser(FpSignupRequest request);
}
