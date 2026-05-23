package com.mallfei.user.domain.service;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.common.security.PasswordCodec;
import com.mallfei.user.application.vo.LoginCaptchaChallengeResult;
import com.mallfei.user.application.vo.LoginCaptchaVerifyResult;
import com.mallfei.user.application.vo.SmsCodeSendResult;
import com.mallfei.user.domain.model.UserAccount;
import com.mallfei.user.domain.model.UserAddress;
import com.mallfei.user.domain.repository.LoginCaptchaRepository;
import com.mallfei.user.domain.repository.LoginSmsCodeRepository;
import com.mallfei.user.domain.repository.UserAccountRepository;
import com.mallfei.user.domain.repository.UserAddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UserDomainService {

    private static final Logger log = LoggerFactory.getLogger(UserDomainService.class);
    private static final int MAX_ADDRESS_COUNT = 20;
    private static final int LOGIN_SMS_EXPIRE_SECONDS = 60;
    private static final int LOGIN_SMS_COOLDOWN_SECONDS = 60;
    private static final int LOGIN_CAPTCHA_EXPIRE_SECONDS = 120;
    private static final int LOGIN_CAPTCHA_VERIFY_EXPIRE_SECONDS = 120;
    private static final int LOGIN_CAPTCHA_TOLERANCE = 8;
    public static final String USER_DISABLED_MESSAGE = "该用户已禁用，详情可咨询平台/客服";
    public static final String USER_DISABLED_CODE = "AUTH_403";

    private final UserAccountRepository userAccountRepository;
    private final UserAddressRepository userAddressRepository;
    private final LoginSmsCodeRepository loginSmsCodeRepository;
    private final LoginCaptchaRepository loginCaptchaRepository;
    private final LoginCaptchaRenderer loginCaptchaRenderer;
    private final PasswordCodec passwordCodec;

    public UserDomainService(UserAccountRepository userAccountRepository,
                             UserAddressRepository userAddressRepository,
                             LoginSmsCodeRepository loginSmsCodeRepository,
                             LoginCaptchaRepository loginCaptchaRepository,
                             LoginCaptchaRenderer loginCaptchaRenderer,
                             PasswordCodec passwordCodec) {
        this.userAccountRepository = userAccountRepository;
        this.userAddressRepository = userAddressRepository;
        this.loginSmsCodeRepository = loginSmsCodeRepository;
        this.loginCaptchaRepository = loginCaptchaRepository;
        this.loginCaptchaRenderer = loginCaptchaRenderer;
        this.passwordCodec = passwordCodec;
    }

    public UserAccount loadByMobile(String mobile) {
        return userAccountRepository.findByMobile(mobile)
                .orElseThrow(() -> BusinessException.badRequest("用户账号不存在"));
    }

    public UserAccount loadById(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> BusinessException.badRequest("用户不存在"));
    }

    public LoginCaptchaChallengeResult createLoginCaptchaChallenge() {
        LoginCaptchaRenderResult renderResult = loginCaptchaRenderer.render();
        String captchaToken = UUID.randomUUID().toString().replace("-", "");
        loginCaptchaRepository.saveChallenge(captchaToken, renderResult.targetOffset(), LOGIN_CAPTCHA_EXPIRE_SECONDS);
        return new LoginCaptchaChallengeResult(
                captchaToken,
                renderResult.backgroundImage(),
                renderResult.sliderImage(),
                renderResult.targetOffset(),
                renderResult.topOffset(),
                renderResult.puzzleSize(),
                LOGIN_CAPTCHA_TOLERANCE,
                LOGIN_CAPTCHA_EXPIRE_SECONDS
        );
    }

    public LoginCaptchaVerifyResult verifyLoginCaptcha(String captchaToken, int offset) {
        Integer targetOffset = loginCaptchaRepository.getTargetOffset(captchaToken);
        if (targetOffset == null) {
            throw BusinessException.badRequest("验证码已过期，请刷新后重试");
        }
        boolean verified = Math.abs(offset - targetOffset) <= LOGIN_CAPTCHA_TOLERANCE;
        if (!verified) {
            throw BusinessException.badRequest("拼图未对齐，请重试");
        }
        String verifyToken = UUID.randomUUID().toString().replace("-", "");
        loginCaptchaRepository.saveVerifiedToken(captchaToken, verifyToken, LOGIN_CAPTCHA_VERIFY_EXPIRE_SECONDS);
        return new LoginCaptchaVerifyResult(true, verifyToken);
    }

    public void consumeVerifiedLoginCaptcha(String captchaToken, String verifyToken) {
        boolean consumed = loginCaptchaRepository.consumeVerifiedToken(captchaToken, verifyToken);
        if (!consumed) {
            throw BusinessException.badRequest("验证码校验无效或已失效，请重新验证");
        }
    }

    public SmsCodeSendResult sendLoginSmsCode(String mobile) {
        if (!loginSmsCodeRepository.canSend(mobile)) {
            throw BusinessException.badRequest("验证码发送过于频繁，请60秒后再试");
        }
        loadByMobile(mobile);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        loginSmsCodeRepository.saveLoginCode(mobile, code, LOGIN_SMS_EXPIRE_SECONDS);
        loginSmsCodeRepository.markSendCooldown(mobile, LOGIN_SMS_COOLDOWN_SECONDS);
        String consoleMessage = String.format("[MOCK_SMS] mobile=%s, loginCode=%s, expireSeconds=%d", mobile, code, LOGIN_SMS_EXPIRE_SECONDS);
        System.out.println("========================================");
        System.out.println("模拟短信验证码已生成");
        System.out.println(consoleMessage);
        System.out.println("========================================");
        System.err.println(consoleMessage);
        log.info(consoleMessage);
        return new SmsCodeSendResult(mobile, LOGIN_SMS_EXPIRE_SECONDS, code);
    }

    public UserAccount loginBySmsCode(String mobile, String code) {
        String cachedCode = loginSmsCodeRepository.getLoginCode(mobile);
        if (cachedCode == null || cachedCode.isBlank()) {
            throw BusinessException.badRequest("验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(code)) {
            throw BusinessException.badRequest("验证码错误");
        }
        loginSmsCodeRepository.deleteLoginCode(mobile);
        UserAccount userAccount = loadByMobile(mobile);
        if (!userAccount.enabled()) {
            throw BusinessException.forbidden(USER_DISABLED_MESSAGE);
        }
        return userAccount;
    }

    public UserAccount updateProfile(Long userId, String nickname, String avatarUrl) {
        String trimmedNickname = nickname == null ? "" : nickname.trim();
        String normalizedAvatarUrl = avatarUrl == null ? "" : avatarUrl.trim();
        if (trimmedNickname.isEmpty()) {
            throw BusinessException.badRequest("昵称不能为空");
        }
        if (trimmedNickname.length() > 20) {
            throw BusinessException.badRequest("昵称长度不能超过20个字符");
        }
        UserAccount existing = loadById(userId);
        boolean nicknameChanged = !trimmedNickname.equals(existing.nickname());
        if (nicknameChanged && userAccountRepository.existsByNicknameAndIdNot(trimmedNickname, userId)) {
            throw BusinessException.badRequest("昵称已被其他用户使用");
        }
        if (normalizedAvatarUrl.length() > 255) {
            throw BusinessException.badRequest("头像地址长度不能超过255个字符");
        }
        return userAccountRepository.update(existing.withProfile(trimmedNickname, normalizedAvatarUrl));
    }

    public UserAccount changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
        UserAccount existing = loadById(userId);
        if (!passwordCodec.matches(oldPassword, existing.passwordHash())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw BusinessException.badRequest("新密码长度需在6到20位之间");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw BusinessException.badRequest("两次输入的新密码不一致");
        }
        if (passwordCodec.matches(newPassword, existing.passwordHash())) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }
        String encodedPassword = passwordCodec.encode(newPassword);
        userAccountRepository.updatePasswordHash(userId, encodedPassword);
        return new UserAccount(existing.id(), existing.mobile(), encodedPassword, existing.nickname(), existing.avatarUrl(), existing.status());
    }

    public List<UserAddress> loadAddresses(Long userId) {
        return userAddressRepository.findByUserId(userId);
    }

    public UserAddress loadOwnedAddress(Long addressId, Long userId) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> BusinessException.badRequest("收货地址不存在"));
        if (!address.belongsTo(userId)) {
            throw BusinessException.forbidden("无权访问当前收货地址");
        }
        return address;
    }

    public void validateLogin(UserAccount userAccount, String rawPassword) {
        if (!passwordCodec.matches(rawPassword, userAccount.passwordHash())) {
            throw BusinessException.badRequest("密码错误");
        }
    }

    public UserAccount register(String mobile, String password, String nickname) {
        if (userAccountRepository.existsByMobile(mobile)) {
            throw BusinessException.badRequest("手机号已注册");
        }
        return userAccountRepository.save(new UserAccount(
                null,
                mobile,
                passwordCodec.encode(password),
                nickname,
                "/images/default-avatar.svg",
                "ENABLED"
        ));
    }

    public UserAccount registerByThirdParty(String nickname, String avatarUrl, String rawPassword) {
        String trimmedNickname = nickname == null || nickname.isBlank() ? "第三方用户" : nickname.trim();
        if (trimmedNickname.length() > 20) {
            trimmedNickname = trimmedNickname.substring(0, 20);
        }
        String normalizedAvatar = (avatarUrl == null || avatarUrl.isBlank()) ? "/images/default-avatar.svg" : avatarUrl.trim();
        return userAccountRepository.save(new UserAccount(
                null,
                null,
                passwordCodec.encode(rawPassword),
                trimmedNickname,
                normalizedAvatar,
                "ENABLED"
        ));
    }

    public UserAddress createAddress(Long userId,
                                     String receiverName,
                                     String receiverPhone,
                                     String provinceCode,
                                     String provinceName,
                                     String cityCode,
                                     String cityName,
                                     String districtCode,
                                     String districtName,
                                     String detailAddress,
                                     String postalCode,
                                     boolean requestedDefault) {
        validateReceiverPhone(receiverPhone);
        long activeCount = userAddressRepository.countActiveByUserId(userId);
        validateAddressCount(activeCount);
        boolean shouldBeDefault = requestedDefault || activeCount == 0;
        if (shouldBeDefault) {
            userAddressRepository.clearDefaultByUserId(userId);
        }
        return userAddressRepository.save(new UserAddress(
                null,
                userId,
                receiverName,
                receiverPhone,
                provinceCode == null ? "" : provinceCode,
                provinceName,
                cityCode == null ? "" : cityCode,
                cityName,
                districtCode == null ? "" : districtCode,
                districtName,
                detailAddress,
                postalCode == null ? "" : postalCode,
                shouldBeDefault
        ));
    }

    public UserAddress updateAddress(UserAddress existing,
                                     String receiverName,
                                     String receiverPhone,
                                     String provinceCode,
                                     String provinceName,
                                     String cityCode,
                                     String cityName,
                                     String districtCode,
                                     String districtName,
                                     String detailAddress,
                                     String postalCode,
                                     boolean isDefault) {
        validateReceiverPhone(receiverPhone);
        if (isDefault) {
            userAddressRepository.clearDefaultByUserId(existing.userId());
        }
        UserAddress updated = existing.apply(
                receiverName,
                receiverPhone,
                provinceCode,
                provinceName,
                cityCode,
                cityName,
                districtCode,
                districtName,
                detailAddress,
                postalCode,
                isDefault
        );
        userAddressRepository.update(updated);
        return loadOwnedAddress(existing.id(), existing.userId());
    }

    public void deleteAddress(UserAddress existing) {
        userAddressRepository.markDeleted(existing.id());
        List<UserAddress> remaining = userAddressRepository.findByUserId(existing.userId());
        boolean hasDefault = remaining.stream().anyMatch(UserAddress::isDefault);
        if (!remaining.isEmpty() && !hasDefault) {
            userAddressRepository.update(remaining.getFirst().markDefault());
        }
    }

    public UserAddress setDefaultAddress(UserAddress existing) {
        userAddressRepository.clearDefaultByUserId(existing.userId());
        userAddressRepository.update(existing.markDefault());
        return loadOwnedAddress(existing.id(), existing.userId());
    }

    private void validateAddressCount(long currentCount) {
        if (currentCount >= MAX_ADDRESS_COUNT) {
            throw BusinessException.badRequest("收货地址最多只能保存20条");
        }
    }

    private void validateReceiverPhone(String receiverPhone) {
        if (receiverPhone == null || !receiverPhone.matches("^1\\d{10}$")) {
            throw BusinessException.badRequest("收货手机号格式不正确");
        }
    }
}
