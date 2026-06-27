package com.mallfei.user.domain.service;

import com.mallfei.common.security.PasswordCodec;
import com.mallfei.testsupport.BaseUnitTest;
import com.mallfei.user.domain.model.UserAccount;
import com.mallfei.user.domain.repository.LoginCaptchaRepository;
import com.mallfei.user.domain.repository.LoginSmsCodeRepository;
import com.mallfei.user.domain.repository.UserAccountRepository;
import com.mallfei.user.domain.repository.UserAddressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("mall-user 用户领域服务纯单元测试")
class UserDomainServiceTest extends BaseUnitTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private LoginSmsCodeRepository loginSmsCodeRepository;

    @Mock
    private LoginCaptchaRepository loginCaptchaRepository;

    @Mock
    private LoginCaptchaRenderer loginCaptchaRenderer;

    @Mock
    private PasswordCodec passwordCodec;

    @InjectMocks
    private UserDomainService userDomainService;

    @Test
    @DisplayName("正向业务流程：短信验证码正确时完成登录并消费验证码")
    void loginBySmsCodeShouldSucceedAndConsumeCode() {
        // Given：Redis 中存在有效登录验证码，且手机号已注册启用账号。
        when(loginSmsCodeRepository.getLoginCode("13800138000")).thenReturn("123456");
        when(userAccountRepository.findByMobile("13800138000")).thenReturn(Optional.of(enabledUser()));

        // When
        UserAccount user = userDomainService.loginBySmsCode("13800138000", "123456");

        // Then：登录成功后必须消费验证码，避免验证码复用。
        assertThat(user.id()).isEqualTo(1L);
        verify(loginSmsCodeRepository).deleteLoginCode("13800138000");
    }

    @Test
    @DisplayName("异常场景：验证码错误时拒绝登录并返回 COMMON_400")
    void loginBySmsCodeShouldRejectWrongCode() {
        // Given：用户输入验证码与服务端保存值不一致。
        when(loginSmsCodeRepository.getLoginCode("13800138000")).thenReturn("123456");

        // When
        Throwable throwable = catchThrowable(() -> userDomainService.loginBySmsCode("13800138000", "654321"));

        // Then：错误验证码不允许消费，便于用户重试。
        assertBadRequest(throwable, "验证码错误");
        verify(loginSmsCodeRepository, never()).deleteLoginCode(any());
    }

    @Test
    @DisplayName("边界值：昵称超过 20 个字符时拒绝更新资料并返回 COMMON_400")
    void updateProfileShouldRejectTooLongNickname() {
        // Given：昵称超过产品规则允许长度。
        String nickname = "一二三四五六七八九十一二三四五六七八九十一";

        // When
        Throwable throwable = catchThrowable(() -> userDomainService.updateProfile(1L, nickname, "avatar.png"));

        // Then：参数异常在领域层拦截，不访问账号仓储。
        assertBadRequest(throwable, "昵称长度不能超过20个字符");
        verifyNoInteractions(userAccountRepository);
    }

    @Test
    @DisplayName("账号绑定：绑定新手机号时校验唯一性、验证码并更新账号")
    void bindMobileShouldValidateCodeAndUpdateAccount() {
        // Given：用户存在、新手机号未被占用、绑定验证码正确。
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(enabledUser()));
        when(userAccountRepository.existsByMobile("13900139000")).thenReturn(false);
        when(loginSmsCodeRepository.getMobileBindCode(1L, "13900139000")).thenReturn("888888");
        when(userAccountRepository.update(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserAccount updated = userDomainService.bindMobile(1L, "13900139000", "888888");

        // Then：更新账号后必须删除绑定验证码，保证一次性。
        assertThat(updated.mobile()).isEqualTo("13900139000");
        verify(userAccountRepository).update(argThat(account -> "13900139000".equals(account.mobile())));
        verify(loginSmsCodeRepository).deleteMobileBindCode(1L, "13900139000");
    }

    @Test
    @DisplayName("并发风险场景：短信发送冷却期内禁止重复发送验证码并返回 COMMON_400")
    void sendLoginSmsCodeShouldRejectCooldownRepeat() {
        // Given：Redis 原子冷却键仍在有效期内。
        when(loginSmsCodeRepository.canSend("13800138000")).thenReturn(false);

        // When
        Throwable throwable = catchThrowable(() -> userDomainService.sendLoginSmsCode("13800138000"));

        // Then：冷却期命中时不得生成或保存新验证码。
        assertBadRequest(throwable, "验证码发送过于频繁");
        verify(loginSmsCodeRepository, never()).saveLoginCode(any(), any(), anyInt());
    }

    private UserAccount enabledUser() {
        return new UserAccount(1L, "13800138000", "HASH", "测试用户", "avatar.png", "ENABLED");
    }
}
