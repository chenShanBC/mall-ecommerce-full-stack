import { useUserStore } from '../stores/user';

export async function requireLogin(router, action) {
  const userStore = useUserStore();
  const isAuthenticated = await userStore.ensureValidSession();
  if (isAuthenticated) {
    return true;
  }

  const redirect = typeof action === 'function' ? action() : action;
  router.push({
    path: '/login',
    query: redirect ? { redirect } : undefined,
  });
  return false;
}
