<template>
  <div class="login-page">
    <div class="login-banner">
      <div class="login-title">欢迎来到 mallFei</div>
      <div class="login-subtitle">支持密码登录、验证码登录与注册</div>
    </div>

    <div class="form-wrapper">
      <van-tabs v-model:active="activeTab" animated>
        <van-tab title="密码登录" name="login-password">
          <van-form @submit="handlePasswordLogin">
            <van-cell-group inset>
              <van-field v-model="loginForm.mobile" name="mobile" label="手机号" placeholder="请输入 13800000000" />
              <van-field v-model="loginForm.password" type="password" name="password" label="密码" placeholder="请输入密码" />
            </van-cell-group>

            <div class="tips">密码登录前需先完成人机验证，验证结果由前后端共同校验。</div>
            <div class="submit-area">
              <van-button round block type="primary" native-type="submit" :loading="loading">登录</van-button>
            </div>
          </van-form>
        </van-tab>

        <van-tab title="验证码登录" name="login-sms">
          <van-form @submit="handleSmsLogin">
            <van-cell-group inset>
              <van-field v-model="smsLoginForm.mobile" name="mobile" label="手机号" placeholder="请输入手机号" />
              <van-field v-model="smsLoginForm.code" name="code" label="验证码" placeholder="请输入6位验证码">
                <template #button>
                  <van-button
                    size="small"
                    plain
                    type="primary"
                    native-type="button"
                    :disabled="smsCountdown > 0"
                    @click.stop.prevent="sendCode"
                  >
                    {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
                  </van-button>
                </template>
              </van-field>
            </van-cell-group>
            <div class="tips">开发阶段会返回调试验证码，正式环境可替换为短信平台。</div>
            <div v-if="debugCode" class="debug-code">调试验证码：{{ debugCode }}</div>
            <div class="submit-area">
              <van-button round block type="primary" native-type="submit" :loading="loading">验证码登录</van-button>
            </div>
          </van-form>
        </van-tab>

        <van-tab title="注册" name="register">
          <van-form @submit="handleRegister">
            <van-cell-group inset>
              <van-field v-model="registerForm.mobile" name="mobile" label="手机号" placeholder="请输入手机号" />
              <van-field v-model="registerForm.nickname" name="nickname" label="昵称" placeholder="请输入昵称" />
              <van-field v-model="registerForm.password" type="password" name="password" label="密码" placeholder="请输入密码" />
            </van-cell-group>
            <div class="tips">注册成功后将自动登录并跳转到首页。</div>
            <div class="submit-area">
              <van-button round block type="success" native-type="submit" :loading="loading">注册并登录</van-button>
            </div>
          </van-form>
        </van-tab>
      </van-tabs>
    </div>

    <div class="third-login-wrap">
      <van-button block plain type="primary" :loading="loading" @click="handleAlipayLogin">
        支付宝快捷登录
      </van-button>
      <div class="third-login-tip">首次登录将自动创建账号并完成绑定</div>
    </div>

    <van-popup v-model:show="showCaptchaPopup" class="captcha-popup" round @closed="handleCaptchaClosed">
      <div class="slider-wrap">
        <div class="slider-title">
          <div>
            <span>请完成下列验证后继续</span>
            <div class="slider-subtitle">拖动滑块，让拼图与缺口严丝合缝</div>
          </div>
          <div class="title-actions">
            <button class="icon-btn" type="button" @click="refreshCaptcha">↻</button>
            <button class="icon-btn close" type="button" @click="showCaptchaPopup = false">×</button>
          </div>
        </div>

          <div class="captcha-stage">
            <div class="img-box" :style="{ backgroundImage: `url(${backgroundImage || ''})` }">
              <div class="img-overlay"></div>
              <div class="captcha-hole" :style="holeStyle"></div>
              <div class="captcha-piece" :style="pieceStyle"></div>
              <div class="stage-highlight"></div>
              <div v-if="captchaErrorVisible" class="error-mask">
                <div class="error-mark">!</div>
                <div class="error-text">拼图未对齐，<br />请重试</div>
              </div>
            </div>
          </div>

          <div class="slide-box" :class="{ success: captchaVerified }">
            <div class="slider-mask" :style="{ width: `${sliderMaskWidth}px` }"></div>
            <div class="slide-text" :class="{ success: captchaVerified }">
              {{ captchaVerified ? '验证通过，正在登录...' : '按住左边按钮拖动完成上方拼图' }}
            </div>
          <div
            class="block"
            :class="{ dragging: startMove, verified: captchaVerified }"
            :style="{ transform: `translate3d(${blockLeft}px, 0, 0)` }"
            @mousedown.prevent="startDrag"
            @touchstart.prevent="touchstart"
            @touchmove.prevent="touchmove"
            @touchend.prevent="touchend"
          >
            <span class="slider-icon">→</span>
          </div>
        </div>

        <div class="captcha-footer-tip">
          系统将校验拖动结果与服务端令牌，请勿刷新页面。
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { showFailToast, showLoadingToast, showSuccessToast } from 'vant';
import { useRoute, useRouter } from 'vue-router';
import {
  exchangeAlipayLoginTicket,
  fetchAlipayLoginAuthUrl,
  loginCaptchaChallenge,
  loginCaptchaVerify,
  sendLoginSmsCode,
} from '../api';
import { useUserStore } from '../stores/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const loading = ref(false);
const activeTab = ref('login-password');
const smsCountdown = ref(0);
const countdownTimer = ref(null);
const debugCode = ref('');
const showCaptchaPopup = ref(false);
const captchaVerified = ref(false);
const captchaErrorVisible = ref(false);
const captchaToken = ref('');
const captchaVerifyToken = ref('');
const backgroundImage = ref('');
const sliderImage = ref('');
const imageWidth = 320;
const imageHeight = 180;
const puzzleSize = ref(52);
const puzzleTop = ref(56);
const captchaTolerance = 8;
const puzzleTargetOffset = ref(180);
const blockWidth = 48;
const startMove = ref(false);
const startX = ref(0);
const blockLeft = ref(0);
const moveX = ref(0);
const movePercent = ref(0);
const loginForm = reactive({
  mobile: '13800000000',
  password: '123456',
});
const smsLoginForm = reactive({
  mobile: '13800000000',
  code: '',
});
const registerForm = reactive({
  mobile: '',
  nickname: '',
  password: '',
});

const pieceStyle = computed(() => ({
  transform: `translate3d(${moveX.value}px, ${puzzleTop.value}px, 0)`,
  width: `${puzzleSize.value}px`,
  height: `${puzzleSize.value}px`,
  backgroundImage: `url(${sliderImage.value})`,
}));
const holeStyle = computed(() => ({
  transform: `translate3d(${puzzleTargetOffset.value}px, ${puzzleTop.value}px, 0)`,
  width: `${puzzleSize.value}px`,
  height: `${puzzleSize.value}px`,
}));
const maxMoveX = computed(() => imageWidth - puzzleSize.value);
const maxBlockLeft = computed(() => imageWidth - blockWidth);
const sliderMaskWidth = computed(() => blockLeft.value + blockWidth / 2);

const redirectTo = () => {
  router.replace(route.query.redirect || '/home');
};

const isValidMobile = (mobile) => /^1\d{10}$/.test(mobile || '');

const startCountdown = (seconds = 60) => {
  smsCountdown.value = seconds;
  clearInterval(countdownTimer.value);
  countdownTimer.value = setInterval(() => {
    if (smsCountdown.value <= 1) {
      clearInterval(countdownTimer.value);
      smsCountdown.value = 0;
      return;
    }
    smsCountdown.value -= 1;
  }, 1000);
};

const resetSlider = () => {
  startMove.value = false;
  blockLeft.value = 0;
  moveX.value = 0;
  movePercent.value = 0;
  captchaVerified.value = false;
  captchaVerifyToken.value = '';
};

const resetCaptcha = () => {
  captchaErrorVisible.value = false;
  resetSlider();
};

const loadCaptchaChallenge = async () => {
  const { data } = await loginCaptchaChallenge();
  captchaToken.value = data.data.captchaToken;
  backgroundImage.value = data.data.backgroundImage;
  sliderImage.value = data.data.sliderImage;
  puzzleTargetOffset.value = data.data.targetOffset;
  puzzleTop.value = data.data.topOffset;
  puzzleSize.value = data.data.puzzleSize;
  resetCaptcha();
};

const refreshCaptcha = async () => {
  try {
    await loadCaptchaChallenge();
  } catch (error) {
    showFailToast(error?.response?.data?.message || '验证码加载失败');
  }
};

const handleCaptchaClosed = () => {
  resetCaptcha();
};

const updatePosition = (distance) => {
  const limitedDistance = Math.min(Math.max(distance, 0), maxBlockLeft.value);
  blockLeft.value = limitedDistance;
  movePercent.value = maxBlockLeft.value === 0 ? 0 : limitedDistance / maxBlockLeft.value;
  moveX.value = Math.round(movePercent.value * maxMoveX.value);
};

const finishCaptcha = async () => {
  try {
    const { data } = await loginCaptchaVerify({
      captchaToken: captchaToken.value,
      offset: moveX.value,
    });
    captchaVerifyToken.value = data.data.verifyToken;
    captchaVerified.value = true;
    captchaErrorVisible.value = false;
    blockLeft.value = Math.round((puzzleTargetOffset.value / maxMoveX.value) * maxBlockLeft.value);
    moveX.value = puzzleTargetOffset.value;
    showSuccessToast('拖拽验证通过');
    showCaptchaPopup.value = false;
    await submitPasswordLogin();
  } catch (error) {
    captchaErrorVisible.value = true;
    resetSlider();
    showFailToast(error?.response?.data?.message || '拼图校验失败');
  }
};

const finishDrag = async () => {
  if (!startMove.value) {
    return;
  }
  startMove.value = false;
  const matched = Math.abs(moveX.value - puzzleTargetOffset.value) <= captchaTolerance;
  if (matched) {
    await finishCaptcha();
    return;
  }
  captchaErrorVisible.value = true;
  resetSlider();
};

const onMouseMove = (event) => {
  if (!startMove.value) {
    return;
  }
  updatePosition(event.pageX - startX.value);
};

const onMouseUp = async () => {
  window.removeEventListener('mousemove', onMouseMove);
  window.removeEventListener('mouseup', onMouseUp);
  await finishDrag();
};

const startDrag = (event) => {
  captchaErrorVisible.value = false;
  startX.value = event.pageX - blockLeft.value;
  startMove.value = true;
  window.addEventListener('mousemove', onMouseMove);
  window.addEventListener('mouseup', onMouseUp);
};

const touchstart = (event) => {
  captchaErrorVisible.value = false;
  startX.value = event.changedTouches[0].screenX - blockLeft.value;
  startMove.value = true;
};

const touchmove = (event) => {
  if (!startMove.value) {
    return;
  }
  updatePosition(event.changedTouches[0].screenX - startX.value);
};

const touchend = async () => {
  await finishDrag();
};

const sendCode = async () => {
  if (!isValidMobile(smsLoginForm.mobile)) {
    showFailToast('请输入正确的手机号');
    return;
  }
  try {
    const { data } = await sendLoginSmsCode({ mobile: smsLoginForm.mobile });
    debugCode.value = data.data?.debugCode || '';
    showSuccessToast('验证码已发送');
    startCountdown(60);
  } catch (error) {
    showFailToast(error?.response?.data?.message || '验证码发送失败');
  }
};

const submitPasswordLogin = async () => {
  try {
    loading.value = true;
    await userStore.login({
      ...loginForm,
      captchaToken: captchaToken.value,
      captchaVerifyToken: captchaVerifyToken.value,
    });
    showSuccessToast('登录成功');
    redirectTo();
  } catch (error) {
    showFailToast(error?.response?.data?.message || '登录失败');
    await refreshCaptcha();
  } finally {
    loading.value = false;
  }
};

const handlePasswordLogin = async () => {
  try {
    showCaptchaPopup.value = true;
    await loadCaptchaChallenge();
  } catch (error) {
    showFailToast(error?.response?.data?.message || '验证码加载失败');
  }
};

const handleSmsLogin = async () => {
  if (!isValidMobile(smsLoginForm.mobile)) {
    showFailToast('请输入正确的手机号');
    return;
  }
  try {
    loading.value = true;
    await userStore.loginWithSms(smsLoginForm);
    showSuccessToast('登录成功');
    redirectTo();
  } catch (error) {
    showFailToast(error?.response?.data?.message || '验证码登录失败');
  } finally {
    loading.value = false;
  }
};

const handleRegister = async () => {
  try {
    loading.value = true;
    await userStore.register(registerForm);
    showSuccessToast('注册并登录成功');
    redirectTo();
  } catch (error) {
    showFailToast(error?.response?.data?.message || '注册失败');
  } finally {
    loading.value = false;
  }
};

const handleAlipayLogin = async () => {
  try {
    loading.value = true;
    const { data } = await fetchAlipayLoginAuthUrl();
    const authUrl = data?.data?.authUrl;
    if (!authUrl) {
      showFailToast('获取支付宝授权地址失败');
      return;
    }
    window.location.href = authUrl;
  } catch (error) {
    showFailToast(error?.response?.data?.message || '支付宝登录暂不可用');
  } finally {
    loading.value = false;
  }
};

const handleAlipayCallback = async () => {
  const loginTicket = route.query.loginTicket;
  const tag = route.query.alipay;
  if (!loginTicket || tag !== 'callback') {
    return;
  }
  const toast = showLoadingToast({
    message: '支付宝登录中...',
    duration: 0,
    forbidClick: true,
  });
  try {
    const { data } = await exchangeAlipayLoginTicket({ loginTicket });
    userStore.token = data.data.token;
    userStore.profile = data.data;
    userStore.profileLoaded = true;
    localStorage.setItem('mall-h5-token', userStore.token);
    showSuccessToast('支付宝登录成功');
    await router.replace('/home');
  } catch (error) {
    showFailToast(error?.response?.data?.message || '支付宝登录失败');
    await router.replace('/login');
  } finally {
    toast.close();
  }
};

onMounted(async () => {
  await handleAlipayCallback();
});

onBeforeUnmount(() => {
  clearInterval(countdownTimer.value);
  window.removeEventListener('mousemove', onMouseMove);
  window.removeEventListener('mouseup', onMouseUp);
});
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eff6ff 0%, #f7f8fa 240px);
}

.login-banner {
  padding: 48px 20px 24px;
  color: #1e3a8a;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
}

.login-subtitle {
  margin-top: 10px;
  font-size: 14px;
  color: #475569;
}

.form-wrapper {
  margin: 0 12px;
  padding: 12px 0 18px;
  background: #fff;
  border-radius: 24px 24px 0 0;
  box-shadow: 0 -4px 24px rgba(15, 23, 42, 0.04);
}

.captcha-panel {
  margin: 16px;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
}

:deep(.captcha-popup) {
  width: min(94vw, 372px);
  overflow: hidden;
  border-radius: 22px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.28);
}

.slider-wrap {
  position: relative;
  width: 100%;
  padding: 18px 18px 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-sizing: border-box;
}

.slider-title {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  font-size: 14px;
  color: #0f172a;
  font-weight: 700;
}

.slider-subtitle {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.title-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  width: 30px;
  height: 30px;
  color: #60a5fa;
  font-size: 22px;
  line-height: 1;
  background: #eff6ff;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
}

.icon-btn.close {
  color: #94a3b8;
  background: #f1f5f9;
}

.captcha-stage {
  margin-top: 14px;
  padding: 10px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 18px;
}

.img-box {
  position: relative;
  width: 320px;
  height: 180px;
  overflow: hidden;
  border-radius: 14px;
  background-color: #dbeafe;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.32);
}

.img-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.08) 0%, rgba(255, 255, 255, 0.02) 48%, rgba(255, 255, 255, 0.12) 100%);
}

.stage-highlight {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 22% 16%, rgba(255, 255, 255, 0.26) 0, transparent 24%), radial-gradient(circle at 84% 24%, rgba(255, 255, 255, 0.18) 0, transparent 18%);
  pointer-events: none;
}

.captcha-hole,
.captcha-piece {
  position: absolute;
  width: 52px;
  height: 52px;
}

.captcha-hole {
  z-index: 1;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.22), 0 0 0 1px rgba(15, 23, 42, 0.06);
}

.captcha-piece {
  z-index: 2;
  background-position: center;
  background-repeat: no-repeat;
  background-size: contain;
  will-change: transform;
}

.captcha-piece::before,
.captcha-piece::after,
.captcha-hole::before,
.captcha-hole::after {
  display: none;
}

.error-mask {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 154px;
  height: 126px;
  margin: auto;
  color: #fff;
  background: rgba(15, 23, 42, 0.68);
  backdrop-filter: blur(4px);
  border-radius: 18px;
}

.error-mark {
  font-size: 46px;
  line-height: 1;
}

.error-text {
  margin-top: 8px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.45;
  text-align: center;
}

.slide-box {
  position: relative;
  width: 320px;
  height: 48px;
  margin-top: 14px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  border-radius: 14px;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.slide-box.success {
  background: #f0fdf4;
  border-color: #86efac;
  box-shadow: 0 10px 24px rgba(34, 197, 94, 0.16);
}

.slider-mask {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.16) 0%, rgba(59, 130, 246, 0.28) 100%);
}

.slide-box.success .slider-mask {
  background: linear-gradient(90deg, rgba(74, 222, 128, 0.28) 0%, rgba(34, 197, 94, 0.34) 100%);
}

.slide-text {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 18px 0 78px;
  font-size: 14px;
  color: #64748b;
}

.slide-text.success {
  color: #166534;
}

.block {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 46px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border-right: 1px solid #dbe3ef;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.10);
  cursor: pointer;
  touch-action: none;
  will-change: transform;
}

.block.dragging,
.block:hover {
  background: linear-gradient(180deg, #60a5fa 0%, #2563eb 100%);
}

.block.verified {
  background: linear-gradient(180deg, #4ade80 0%, #16a34a 100%);
}

.slider-icon {
  color: #2563eb;
  font-size: 24px;
  font-weight: 700;
}

.block.dragging .slider-icon,
.block:hover .slider-icon,
.block.verified .slider-icon {
  color: #fff;
}

.captcha-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.captcha-footer-tip {
  margin-top: 12px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
}

.captcha-tip,
.tips {
  color: #64748b;
  font-size: 13px;
}

.tips {
  padding: 16px;
}

.debug-code {
  margin: 0 16px 12px;
  padding: 10px 12px;
  color: #1d4ed8;
  font-size: 13px;
  background: #eff6ff;
  border-radius: 12px;
}

.submit-area {
  padding: 0 16px 16px;
}

.third-login-wrap {
  margin: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 16px;
}

.third-login-tip {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  text-align: center;
}
</style>
