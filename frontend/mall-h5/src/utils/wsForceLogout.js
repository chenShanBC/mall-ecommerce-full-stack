import { showFailToast } from 'vant';

export const FORCE_LOGOUT_GUEST_REFRESH_KEY = 'mallfei:force-logout-guest-refreshing';

let socket = null;
let refreshTimer = null;
let handled = false;

const getWsUrl = (token) => {
  const configured = import.meta.env.VITE_WS_URL || '';
  const baseUrl = configured || `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/force-logout`;
  const separator = baseUrl.includes('?') ? '&' : '?';
  return `${baseUrl}${separator}token=${encodeURIComponent(token)}`;
};

export const initForceLogoutSocket = (userStore) => {
  if (socket || typeof window === 'undefined') return;
  const token = userStore.token?.trim();
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
      if (refreshTimer) {
        clearTimeout(refreshTimer);
      }
      showFailToast({
        message: payload?.message || '您的账号已被禁用，即将退出登录。您可以继续以游客身份浏览，或联系客服咨询。',
        duration: 2600,
      });
      sessionStorage.setItem(FORCE_LOGOUT_GUEST_REFRESH_KEY, '1');
      userStore.clearSession();
      if (refreshTimer) {
        clearTimeout(refreshTimer);
      }
      refreshTimer = window.setTimeout(() => {
        window.location.reload();
      }, 2600);
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

export const destroyForceLogoutSocket = () => {
  if (refreshTimer) {
    clearTimeout(refreshTimer);
    refreshTimer = null;
  }
  handled = false;
  if (socket) {
    socket.close();
    socket = null;
  }
};
