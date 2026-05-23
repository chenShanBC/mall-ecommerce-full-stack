import { showFailToast } from 'vant';
import { useUserStore } from '../stores/user';

let redirectingDisabled = false;
let redirectingInvalid = false;

export const redirectLoginWithNotice = (router, message = '您当前帐号已被禁用，即将返回登录页', ms = 2200) => {
  if (redirectingDisabled) return;
  redirectingDisabled = true;
  showFailToast({ message, duration: ms });
  window.setTimeout(() => {
    router.replace('/login');
    redirectingDisabled = false;
  }, ms);
};

export const redirectLoginForInvalidSession = (router, message = '登录已失效，请重新登录', ms = 1200, redirect) => {
  if (redirectingInvalid) return;
  redirectingInvalid = true;
  showFailToast({ message, duration: ms });
  window.setTimeout(() => {
    router.replace({ path: '/login', query: redirect ? { redirect } : undefined });
    redirectingInvalid = false;
  }, ms);
};

export async function ensureAccessGate(router, action, options = {}) {
  const { force = false } = options;
  const userStore = useUserStore();
  const redirect = typeof action === 'function' ? action() : action;

  if (!userStore.token?.trim()) {
    redirectLoginForInvalidSession(router, '请先登录后再操作', 1200, redirect);
    return false;
  }

  const isAuthenticated = await userStore.ensureValidSession(force || !userStore.profileLoaded);
  if (isAuthenticated) {
    return true;
  }

  const message = userStore.lastSessionInvalidReason === 'DISABLED'
    ? '您当前帐号已被禁用，即将返回登录页'
    : '登录已失效，请重新登录';
  if (userStore.lastSessionInvalidReason === 'DISABLED') {
    redirectLoginWithNotice(router, message);
    return false;
  }

  redirectLoginForInvalidSession(router, message, 1200, redirect);
  return false;
}

export async function requireLogin(router, action, options = {}) {
  return ensureAccessGate(router, action, options);
}
