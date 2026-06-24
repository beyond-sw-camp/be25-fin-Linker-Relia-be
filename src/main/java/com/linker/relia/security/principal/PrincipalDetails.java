package com.linker.relia.security.principal;

import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class PrincipalDetails implements UserDetails {
    private final User user;

    public static PrincipalDetails from(User user) {
        return new PrincipalDetails(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getLoginId();
    }

    // 계정 활성화 여부
    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    // 계정 잠금 여부
    @Override
    public boolean isAccountNonLocked() {
        return user.getUserStatus() != UserStatus.RESIGNED;
    }
}
