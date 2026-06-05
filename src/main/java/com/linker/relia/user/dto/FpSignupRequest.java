package com.linker.relia.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FpSignupRequest {
    @NotBlank(message = "로그인 ID는 필수입니다.")
    @Size(max = 100, message = "로그인 ID는 100자 이하여야 합니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 255, message = "비밀번호는 8자 이상 255자 이하여야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String userName;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    private String email;

    @Pattern(regexp = "^$|^[0-9-]{9,30}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phone;

    @NotBlank(message = "조직 코드는 필수입니다.")
    @Size(max = 50, message = "조직 코드는 50자 이하여야 합니다.")
    private String organizationCode;
}
