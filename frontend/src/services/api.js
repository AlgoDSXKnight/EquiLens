const viteEnv = import.meta.env ?? {};

export const RESEARCH_API = viteEnv.VITE_RESEARCH_API_URL || 'http://localhost:8080/api';
export const PORTFOLIO_API = viteEnv.VITE_PORTFOLIO_API_URL || 'http://localhost:8081/api';
export const API_BASE = RESEARCH_API;
export const COMPANIES_API = `${RESEARCH_API}/companies`;

export const TOKEN_KEY = 'equilens_token';
export const USER_KEY = 'equilens_user';

export async function apiFetch(url, options = {}) {
  const token = localStorage.getItem(TOKEN_KEY);
  const headers = { ...(options.headers || {}) };

  if (options.body) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(url, { ...options, headers });
  if (response.status === 401) {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    window.dispatchEvent(new Event('equilens:unauthorized'));
    throw new Error('SESSION_EXPIRED');
  }
  return response;
}

export async function publicApiFetch(url, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body) headers['Content-Type'] = 'application/json';
  return fetch(url, { ...options, headers });
}
