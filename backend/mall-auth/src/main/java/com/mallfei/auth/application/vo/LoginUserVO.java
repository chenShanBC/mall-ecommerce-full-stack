package com.mallfei.auth.application.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginUserVO {

    private String token;
    private String account;
    private String loginType;
    private String nickname;
}
