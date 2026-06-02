package com.linker.relia.auth.service;

import com.linker.relia.auth.dto.ReissueResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    ReissueResponse reissueToken(String refreshToken, HttpServletResponse response);
    void logout(String bearerToken, String refreshToken, HttpServletResponse response);
}
