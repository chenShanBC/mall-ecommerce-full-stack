const CART_FALLBACK_KEY = 'mall-h5-cart-fallback';

const readFallbackCart = () => {
  try {
    const raw = localStorage.getItem(CART_FALLBACK_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const writeFallbackCart = (items) => {
  localStorage.setItem(CART_FALLBACK_KEY, JSON.stringify(items));
};

const normalizeItem = (item = {}) => ({
  id: item.id ?? `local-${item.skuId}`,
  skuId: item.skuId,
  spuId: item.spuId ?? null,
  productName: item.productName || '',
  productImage: item.productImage || '',
  skuName: item.skuName || '',
  specJson: item.specJson || '{}',
  unitPrice: Number(item.unitPrice || 0),
  quantity: Number(item.quantity || 1),
  subtotalAmount: Number(item.subtotalAmount || Number(item.unitPrice || 0) * Number(item.quantity || 1)),
  checked: item.checked !== false,
  canCheckout: item.canCheckout !== false,
  invalidReason: item.invalidReason || null,
  createdAt: item.createdAt || new Date().toISOString(),
  isLocalFallback: true,
});

export const getFallbackCartItems = () => readFallbackCart();

export const saveFallbackCartItem = (item) => {
  const current = readFallbackCart();
  const normalized = normalizeItem(item);
  const index = current.findIndex((candidate) => String(candidate.skuId) === String(normalized.skuId));

  if (index >= 0) {
    const existing = normalizeItem(current[index]);
    const merged = {
      ...existing,
      ...normalized,
      quantity: existing.quantity + normalized.quantity,
    };
    merged.subtotalAmount = merged.unitPrice * merged.quantity;
    current[index] = merged;
  } else {
    current.unshift(normalized);
  }

  writeFallbackCart(current);
  return current;
};

export const removeFallbackCartItem = (itemId) => {
  const next = readFallbackCart().filter((item) => String(item.id) !== String(itemId));
  writeFallbackCart(next);
  return next;
};

export const clearFallbackCart = () => {
  writeFallbackCart([]);
};

export const updateFallbackCartItem = (itemId, patch = {}) => {
  const next = readFallbackCart().map((item) => {
    if (String(item.id) !== String(itemId)) {
      return item;
    }
    const merged = {
      ...normalizeItem(item),
      ...patch,
    };
    merged.subtotalAmount = Number(merged.unitPrice || 0) * Number(merged.quantity || 1);
    return merged;
  });
  writeFallbackCart(next);
  return next;
};

export const mergeCartItems = (remoteItems = []) => {
  const fallbackItems = readFallbackCart().map((item) => normalizeItem(item));
  if (!fallbackItems.length) {
    return remoteItems;
  }

  const remoteSkuIds = new Set(remoteItems.map((item) => String(item.skuId)));
  const remainingFallback = fallbackItems.filter((item) => !remoteSkuIds.has(String(item.skuId)));

  if (remainingFallback.length !== fallbackItems.length) {
    writeFallbackCart(remainingFallback);
  }

  return [...remoteItems, ...remainingFallback];
};
