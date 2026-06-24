package com.linker.relia.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    // 쿠키 생성 메서드
    public Cookie createCookie(String key, String value, int days) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(days * 24 * 60 * 60); // 1일
        cookie.setPath("/");        // 쿠키가 사이트의 모든 경로에서 유효하도록 설정
        cookie.setHttpOnly(true);
        cookie.setSecure(false);        // 로컬 개발 환경에서는 false로 설정 (HTTPS 연결이 필요 없는 경우)
        return cookie;
    }

    // 요청 쿠키에서 지정한 이름의 값을 조회한다.
    // 현재는 사용하지 않지만, 쿠키를 직접 조회해야 하는 경우를 위해 유지한다.
    public String getValue(HttpServletRequest request, String key) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (key.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public Cookie deleteCookie(String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}
