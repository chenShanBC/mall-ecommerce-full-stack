package com.mallfei.auth.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.mallfei.auth.application.dto.LoginRequest;
import com.mallfei.auth.application.vo.LoginUserVO;
import com.mallfei.common.api.ResultCode;
import com.mallfei.common.exception.BizException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthApplicationService {

    public LoginUserVO login(LoginRequest request) {
        if (!("13800000000".equals(request.getAccount()) || "admin".equals(request.getAccount()))) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "账号不存在，请使用示例账号登录");
        }
        if (!"123456".equals(request.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "密码错误，示例密码为 123456");
        }

        StpUtil.login(request.getAccount());
        String nickname = "admin".equals(request.getAccount()) ? "系统管理员" : "示例用户";
        String loginType = "admin".equals(request.getAccount()) ? "admin" : "user";
        return new LoginUserVO(StpUtil.getTokenValue(), request.getAccount(), loginType, nickname);
    }

    public Map<String, Object> currentUser() {
        Map<String, Object> result = new HashMap<>();
        Object loginId = StpUtil.getLoginIdDefaultNull();
        result.put("loginId", loginId);
        result.put("token", StpUtil.getTokenValue());
        result.put("nickname", "admin".equals(String.valueOf(loginId)) ? "系统管理员" : "示例用户");
        return result;
    }
}
