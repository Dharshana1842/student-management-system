(function attachAuthClient(root, factory) {
  const api = factory();
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = api;
  }
  root.AuthClient = api;
}(typeof globalThis !== 'undefined' ? globalThis : this, function createAuthClientModule() {
  function createAuthClient(apiBaseUrl = '', fetchImplementation = globalThis.fetch) {
    const baseUrl = String(apiBaseUrl).trim().replace(/\/+$/, '');
    let csrfToken = null;

    async function refreshCsrfToken() {
      const response = await fetchImplementation(`${baseUrl}/api/auth/csrf`, {
        method: 'GET',
        credentials: 'include',
        headers: { Accept: 'application/json' },
      });
      if (!response.ok) {
        throw new Error('Unable to initialize secure request protection.');
      }
      const payload = await response.json();
      csrfToken = payload.token;
      if (typeof csrfToken !== 'string' || csrfToken.length === 0) {
        throw new Error('The server did not provide a CSRF token.');
      }
      return csrfToken;
    }

    async function request(path, options = {}) {
      const method = String(options.method || 'GET').toUpperCase();
      const headers = { ...(options.headers || {}) };
      if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
        if (!csrfToken) {
          await refreshCsrfToken();
        }
        headers['X-CSRF-TOKEN'] = csrfToken;
      }
      return fetchImplementation(`${baseUrl}${path}`, {
        ...options,
        method,
        headers,
        credentials: 'include',
      });
    }

    return Object.freeze({
      request,
      refreshCsrfToken,
      clearCsrfToken() {
        csrfToken = null;
      },
    });
  }

  return Object.freeze({ createAuthClient });
}));
