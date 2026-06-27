let socket = null;
let redirectTimer = null;
let handled = false;

export const ADMIN_FORCE_LOGOUT_MESSAGE_KEY = 'mall-admin-force-logout-message';

const getWsUrl = (token) => {
  const configured = import.meta.env.VITE_ADMIN_WS_URL || import.meta.env.VITE_WS_URL || '';
  const fallback = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/admin/force-logout`;
  const baseUrl = configured || fallback;
  const finalUrl = configured && configured.endsWith('/ws/force-logout') ? configured.replace('/ws/force-logout', '/ws/admin/force-logout') : baseUrl;
  const separator = finalUrl.includes('?') ? '&' : '?';
  return `${finalUrl}${separator}token=${encodeURIComponent(token)}`;
};

export const initAdminForceLogoutSocket = (adminStore, router) => {
  if (socket || typeof window === 'undefined') return;
  const token = adminStore.token?.trim();
  if (!token) return;
  try {
    socket = new WebSocket(getWsUrl(token));
  } catch {
    socket = null;
    return;
  }

  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data || '{}');
      if (payload?.type !== 'forceLogout' || handled) return;
      handled = true;
      if (redirectTimer) {
        clearTimeout(redirectTimer);
      }
      const message = payload?.message || '您的后台账号已被禁用，请重新登录或联系超级管理员处理。';
      localStorage.setItem(ADMIN_FORCE_LOGOUT_MESSAGE_KEY, message);
      localStorage.removeItem('mall-admin-token');
      localStorage.removeItem('mall-admin-profile');
      adminStore.token = '';
      adminStore.profile = null;
      adminStore.dashboard = null;
      adminStore.profileLoaded = false;
      adminStore.profileLoadingPromise = null;
      adminStore.sessionCheckPromise = null;
      router.replace({ path: '/login', query: { reason: 'forceLogout', t: Date.now() } }).catch(() => null);
      redirectTimer = window.setTimeout(() => {
        const loginPath = `${import.meta.env.BASE_URL.replace(/\/$/, '')}/login`;
        window.location.replace(`${loginPath}?reason=forceLogout&t=${Date.now()}`);
      }, 80);
    } catch {
    }
  };

  socket.onclose = () => {
    socket = null;
  };

  socket.onerror = () => {
    socket = null;
  };
};

export const destroyAdminForceLogoutSocket = () => {
  if (redirectTimer) {
    clearTimeout(redirectTimer);
    redirectTimer = null;
  }
  handled = false;
  if (socket) {
    socket.close();
    socket = null;
  }
};
