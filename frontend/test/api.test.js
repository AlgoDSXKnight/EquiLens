import { afterEach, describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  API_BASE,
  PORTFOLIO_API,
  RESEARCH_API,
  TOKEN_KEY,
  USER_KEY,
  apiFetch,
  publicApiFetch
} from '../src/services/api.js';

const originalFetch = globalThis.fetch;
const originalStorage = globalThis.localStorage;
const originalWindow = globalThis.window;

function installStorage(values = {}) {
  const storage = new Map(Object.entries(values));
  globalThis.localStorage = {
    getItem: key => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, String(value)),
    removeItem: key => storage.delete(key)
  };
  return storage;
}

afterEach(() => {
  globalThis.fetch = originalFetch;
  if (originalStorage === undefined) delete globalThis.localStorage;
  else globalThis.localStorage = originalStorage;
  if (originalWindow === undefined) delete globalThis.window;
  else globalThis.window = originalWindow;
});

describe('API service', () => {
  it('provides independent service URLs and a research base alias', () => {
    assert.equal(API_BASE, RESEARCH_API);
    assert.match(RESEARCH_API, /:8080\/api$/);
    assert.match(PORTFOLIO_API, /:8081\/api$/);
  });

  it('adds the saved JWT and JSON content type to protected requests', async () => {
    installStorage({ [TOKEN_KEY]: 'signed-token' });
    let received;
    globalThis.fetch = async (_url, options) => {
      received = options;
      return new Response('{}', { status: 200 });
    };

    await apiFetch(`${PORTFOLIO_API}/portfolio/transactions`, {
      method: 'POST',
      body: JSON.stringify({ type: 'BUY' })
    });

    assert.equal(received.headers.Authorization, 'Bearer signed-token');
    assert.equal(received.headers['Content-Type'], 'application/json');
  });

  it('clears the session and emits the global unauthorized event on 401', async () => {
    const storage = installStorage({ [TOKEN_KEY]: 'expired', [USER_KEY]: '{}' });
    const events = [];
    globalThis.window = { dispatchEvent: event => events.push(event.type) };
    globalThis.fetch = async () => new Response('', { status: 401 });

    await assert.rejects(apiFetch(`${PORTFOLIO_API}/auth/me`), /SESSION_EXPIRED/);
    assert.equal(storage.has(TOKEN_KEY), false);
    assert.equal(storage.has(USER_KEY), false);
    assert.deepEqual(events, ['equilens:unauthorized']);
  });

  it('does not attach a JWT to public authentication requests', async () => {
    installStorage({ [TOKEN_KEY]: 'must-not-leak' });
    let received;
    globalThis.fetch = async (_url, options) => {
      received = options;
      return new Response('{}', { status: 200 });
    };

    await publicApiFetch(`${PORTFOLIO_API}/auth/login`, {
      method: 'POST',
      body: JSON.stringify({ email: 'person@example.com' })
    });

    assert.equal(received.headers.Authorization, undefined);
    assert.equal(received.headers['Content-Type'], 'application/json');
  });
});
