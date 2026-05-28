const TOKEN_KEY = "sllp.accessToken";

export function getStoredAccessToken() {
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setStoredAccessToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearStoredAccessToken() {
  window.localStorage.removeItem(TOKEN_KEY);
}
