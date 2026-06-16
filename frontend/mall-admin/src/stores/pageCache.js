import { reactive } from 'vue';

export const PAGE_CACHE_TTL = 60 * 1000;

export const adminPageCache = reactive({
  dashboard: {
    overview: {},
    products: [],
    productStats: { total: 0, online: 0, offline: 0 },
    warnings: [],
    loaded: false,
    updatedAt: 0,
  },
  products: {
    filters: { keyword: '', categoryId: null, status: '', salesBand: '', hotSalesThreshold: 100, lowSalesThreshold: 10 },
    query: { sortBy: 'id', sortOrder: 'desc' },
    pager: { page: 1, size: 10, total: 0 },
    list: [],
    categories: [],
    loaded: false,
    updatedAt: 0,
    categoriesLoadedAt: 0,
  },
  stocks: {
    filters: { skuId: '', stockStatus: '', warningStatus: '' },
    query: { sortBy: '', sortOrder: '' },
    pager: { page: 1, size: 10, total: 0 },
    list: [],
    loaded: false,
    updatedAt: 0,
  },
});

export function isCacheFresh(updatedAt, ttl = PAGE_CACHE_TTL) {
  return Boolean(updatedAt) && Date.now() - updatedAt < ttl;
}
