const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { createAuthClient } = require('../frontend/auth-client');

test('API base URL follows the page origin', () => {
  const window = { location: { origin: 'http://localhost:8080' } };
  const configSource = fs.readFileSync(require.resolve('../frontend/config.js'), 'utf8');

  vm.runInNewContext(configSource, { window });

  assert.equal(window.APP_CONFIG.apiBaseUrl, 'http://localhost:8080');
});

test('unsafe requests carry the in-memory CSRF token and cookies', async () => {
  const calls = [];
  const client = createAuthClient('', async (url, options) => {
    calls.push({ url, options });
    if (url === '/api/auth/csrf') {
      return { ok: true, json: async () => ({ token: 'csrf-test-token' }) };
    }
    return { ok: true, status: 201 };
  });

  await client.request('/api/auth/register', { method: 'POST', body: '{}' });

  assert.equal(calls.length, 2);
  assert.equal(calls[0].options.credentials, 'include');
  assert.equal(calls[1].options.credentials, 'include');
  assert.equal(calls[1].options.headers['X-CSRF-TOKEN'], 'csrf-test-token');
});

test('safe requests do not need a CSRF token', async () => {
  const calls = [];
  const client = createAuthClient('https://api.example.test', async (url, options) => {
    calls.push({ url, options });
    return { ok: true, json: async () => ({}) };
  });

  await client.request('/api/auth/session');
  assert.equal(calls.length, 1);
  assert.equal(calls[0].url, 'https://api.example.test/api/auth/session');
  assert.equal(calls[0].options.credentials, 'include');
  assert.equal(calls[0].options.headers['X-CSRF-TOKEN'], undefined);
});

test('clearing the token fetches a fresh token before the next mutation', async () => {
  let tokenNumber = 0;
  const calls = [];
  const client = createAuthClient('', async (url, options) => {
    calls.push({ url, options });
    if (url === '/api/auth/csrf') {
      tokenNumber += 1;
      return { ok: true, json: async () => ({ token: `token-${tokenNumber}` }) };
    }
    return { ok: true };
  });

  await client.request('/api/auth/login', { method: 'POST', body: '{}' });
  client.clearCsrfToken();
  await client.request('/api/auth/logout', { method: 'POST' });

  assert.equal(calls[1].options.headers['X-CSRF-TOKEN'], 'token-1');
  assert.equal(calls[3].options.headers['X-CSRF-TOKEN'], 'token-2');
});
