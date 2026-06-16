const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');

const backendOrigin = (() => {
  const devApiTarget = (import.meta.env.VITE_DEV_API_TARGET || '').replace(/\/$/, '');
  if (devApiTarget) return devApiTarget;
  if (/^https?:\/\//i.test(apiBaseUrl)) return apiBaseUrl.replace(/\/api$/, '');
  return window.location.origin;
})();

const assetBaseUrl = (() => {
  const explicit = (import.meta.env.VITE_ASSET_BASE_URL || '').replace(/\/$/, '');
  if (explicit) return explicit;
  return backendOrigin;
})();

const isBackendHost = (host) => ['42.194.224.180', 'localhost', '127.0.0.1', window.location.hostname].includes(host);

export const normalizeUploadPath = (url, kind = 'product') => {
  if (!url) return url;
  const value = String(url).trim();
  if (!value) return value;
  if (value.startsWith('data:image/')) return value;

  const withAssetOrigin = (path) => `${assetBaseUrl}${path.startsWith('/') ? '' : '/'}${path}`;

  if (/^https?:\/\//i.test(value)) {
    try {
      const parsed = new URL(value);
      const normalizedPath = parsed.pathname
        .replace(/^\/api\/uploads\//i, '/uploads/')
        .replace(/^\/upload\//i, '/uploads/');
      if (isBackendHost(parsed.hostname) && normalizedPath.startsWith('/uploads/')) {
        return `${assetBaseUrl}${normalizedPath}${parsed.search || ''}`;
      }
      return value;
    } catch {
      return value;
    }
  }

  let normalized = value
    .replace(/^\/api\/uploads\//i, '/uploads/')
    .replace(/^\/upload\//i, '/uploads/')
    .replace(/^uploads\//i, '/uploads/')
    .replace(/^upload\//i, '/uploads/');

  if (normalized.startsWith('/uploads/')) return withAssetOrigin(normalized);
  if (normalized.startsWith('/upload/')) return withAssetOrigin(normalized.replace(/^\/upload\//i, '/uploads/'));
  if (normalized.startsWith('/api/uploads/')) return withAssetOrigin(normalized.replace(/^\/api\/uploads\//i, '/uploads/'));
  if (normalized.startsWith('/')) return normalized;
  if (/\.(png|jpe?g|webp|gif|svg)(\?.*)?$/i.test(normalized)) return withAssetOrigin(`/uploads/${kind}/${normalized}`);
  return normalized;
};
