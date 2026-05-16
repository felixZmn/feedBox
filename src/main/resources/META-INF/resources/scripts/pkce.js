"use strict";

let config;
const scope = "openid profile offline_access";
let refreshPromise = null; // Promise lock to prevent parallel refresh requests

const endpoints = {
  get authorization() {
    return `${config.authServerUrl}authorize/`;
  },
  get token() {
    return `${config.authServerUrl}token/`;
  },
  get userinfo() {
    return `${config.authServerUrl}userinfo/`;
  },
};

async function initSSOConfig() {
  try {
    const response = await fetch("/api/config");
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const json = await response.json();
    config = {
      clientId: json.clientId,
      redirectUri: json.redirectUri,
      authServerUrl: json.authServerUrl,
      endSessionEndpoint: json.endSessionEndpoint,
    };
  } catch (error) {
    console.error("Error fetching SSO configuration:", error);
    throw error;
  }
}

/**
 * Generates a cryptographically strong random string.
 * @param {number} length - The length of the string to generate.
 * @returns {string} The generated random string.
 */
function generateRandomString(length = 32) {
  const array = new Uint8Array(length);
  crypto.getRandomValues(array);
  // Encode directly to base64, then convert to base64url (no modulo bias)
  return btoa(String.fromCharCode(...array))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

/**
 * Generates a PKCE code challenge from a code verifier.
 * @param {string} verifier - The code verifier.
 * @returns {Promise<string>} The generated code challenge.
 */
async function generateCodeChallenge(verifier) {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

/**
 * In-memory token store.
 * Handles access token, refresh token, and expiration time.
 * Provides methods to restore, save, clear, and check expiration.
 */
const memoryStore = {
  access_token: null,
  refresh_token: null,
  expires_at: null,

  restore() {
    this.access_token = sessionStorage.getItem("access_token") || null;
    this.expires_at = Number(sessionStorage.getItem("expires_at")) || null;
    this.refresh_token = localStorage.getItem("refresh_token") || null;
  },

  save(response) {
    const { access_token, refresh_token, expires_in } = response;
    if (access_token) {
      this.access_token = access_token;
      sessionStorage.setItem("access_token", access_token);
    }
    if (refresh_token) {
      this.refresh_token = refresh_token;
      localStorage.setItem("refresh_token", refresh_token);
    }
    if (expires_in) {
      this.expires_at = Date.now() + expires_in * 1000;
      sessionStorage.setItem("expires_at", this.expires_at.toString());
    }
  },

  clear() {
    this.access_token = null;
    this.refresh_token = null;
    this.expires_at = null;
    sessionStorage.removeItem("access_token");
    sessionStorage.removeItem("expires_at");
    localStorage.removeItem("refresh_token");
  },

  isExpired() {
    return this.expires_at == null || Date.now() >= this.expires_at;
  },
};
memoryStore.restore();

/**
 * Current token state accessor. Provides real-time access to the current token values and expiration status.
 * This is a read-only interface to the token state, ensuring that all updates go through the memoryStore methods.
 */
const currentToken = {
  get access_token() {
    return memoryStore.access_token;
  },
  get refresh_token() {
    return memoryStore.refresh_token;
  },
  get expires_at() {
    return memoryStore.expires_at;
  },
  get expired() {
    return memoryStore.isExpired();
  },
};

/**
 * Secure Token Refresh with Race-Condition Protection
 * @param {boolean} force - Whether to force a token refresh regardless of expiration.
 * @returns {Promise<void>}
 */
async function handleTokenRefresh(force = false) {
  // Skip if not expired (unless forced by a 401 response)
  if (!force && !memoryStore.isExpired()) return;

  // If a refresh is already in progress, wait for it to finish
  if (refreshPromise) {
    await refreshPromise;
    return;
  }

  // Start a new refresh and cache the promise
  refreshPromise = refreshAccessToken()
    .catch(async (error) => {
      console.error("Token refresh failed, redirecting to login:", error);
      memoryStore.clear();
      // Fallback to redirect if config isn't loaded yet
      window.location.href = config?.redirectUri || "/";
      throw error;
    })
    .finally(() => {
      refreshPromise = null;
    });

  await refreshPromise;
}

/**
 * Core OAuth functions
 */
async function redirectToAuthProvider() {
  const codeVerifier = generateRandomString(64);
  sessionStorage.setItem("code_verifier", codeVerifier);

  const codeChallenge = await generateCodeChallenge(codeVerifier);
  const state = generateRandomString(32);
  sessionStorage.setItem("oauth_state", state);
  const nonce = generateRandomString(32);
  sessionStorage.setItem("oidc_nonce", nonce);

  const authUrl = new URL(endpoints.authorization);
  const params = {
    response_type: "code",
    client_id: config.clientId,
    scope: scope,
    code_challenge_method: "S256",
    code_challenge: codeChallenge,
    state: state,
    nonce: nonce,
    redirect_uri: config.redirectUri,
  };
  authUrl.search = new URLSearchParams(params).toString();
  window.location.href = authUrl.toString();
}

/**
 * Exchanges an authorization code for access and refresh tokens.
 * @param {string} code - The authorization code.
 * @returns {Promise<Object>} The token data.
 */
async function exchangeCodeForToken(code) {
  const storedState = sessionStorage.getItem("oauth_state");
  const urlParams = new URLSearchParams(window.location.search);
  const returnedState = urlParams.get("state");
  if (returnedState !== storedState) {
    throw new Error("Invalid state parameter - possible CSRF attack");
  }

  const codeVerifier = sessionStorage.getItem("code_verifier");
  if (!codeVerifier) {
    throw new Error(
      "No code_verifier found - possible replay or misconfiguration",
    );
  }

  const response = await fetch(endpoints.token, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: config.clientId,
      grant_type: "authorization_code",
      code: code,
      redirect_uri: config.redirectUri,
      code_verifier: codeVerifier,
    }),
  });

  sessionStorage.removeItem("code_verifier");
  sessionStorage.removeItem("oauth_state");
  sessionStorage.removeItem("oidc_nonce");

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Token exchange failed: ${response.status} ${errorText}`);
  }

  const tokenData = await response.json();
  memoryStore.save(tokenData);

  const cleanUrl = new URL(window.location.href);
  ["code", "state"].forEach((p) => cleanUrl.searchParams.delete(p));
  window.history.replaceState({}, document.title, cleanUrl.href);

  return tokenData;
}

/**
 * Refreshes the access token using the refresh token.
 * @param {boolean} force - Whether to force a token refresh regardless of expiration.
 * @returns {Promise<Object>} The refreshed token data.
 */
async function refreshAccessToken() {
  const refreshToken = memoryStore.refresh_token;
  if (!refreshToken) {
    throw new Error("No refresh token available - re-authentication required");
  }

  const response = await fetch(endpoints.token, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: config.clientId,
      grant_type: "refresh_token",
      refresh_token: refreshToken,
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Token refresh failed: ${response.status} ${errorText}`);
  }

  const tokenData = await response.json();
  memoryStore.save(tokenData);
  return tokenData;
}

/**
 * Fetches user information from the API.
 * @returns {Promise<Object>} The user data.
 */
async function getUserData() {
  const response = await fetchWithAuth(endpoints.userinfo, { method: "GET" });
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`UserInfo request failed: ${response.status} ${errorText}`);
  }
  return await response.json();
}

/**
 * Initializes authentication state on app load. Handles OAuth callback if authorization code is present in the URL.
 * If a code is found, it attempts to exchange it for tokens. If the exchange fails, it logs the error and rethrows it.
 * This function should be called once on app startup to ensure the authentication state is correctly established.
 */
async function initializeAuth() {
  const args = new URLSearchParams(window.location.search);
  const code = args.get("code");
  if (code) {
    try {
      await exchangeCodeForToken(code);
    } catch (error) {
      console.error("Authentication callback failed:", error);
      throw error;
    }
  } else if (memoryStore.refresh_token && memoryStore.isExpired()) {
    // Returning user: access token gone/expired, but refresh token survived
    try {
      await refreshAccessToken();
    } catch (error) {
      // Refresh token also expired or revoked → require login
      console.warn("Silent refresh failed, login required:", error);
      memoryStore.clear();
    }
  }
}

/**
 * Checks if the user is currently authenticated.
 * @returns {boolean} True if the user is authenticated, false otherwise.
 */
function isAuthenticated() {
  return !!memoryStore.access_token && !memoryStore.isExpired();
}

/**
 * Logs the user out by clearing all token data and redirecting to the OIDC provider's end_session_endpoint
 * (if configured), or falling back to the local redirect URI.
 */
function logout() {
  memoryStore.clear();
  sessionStorage.removeItem("code_verifier");
  sessionStorage.removeItem("oauth_state");
  sessionStorage.removeItem("oidc_nonce");

  if (config?.endSessionEndpoint) {
    const url = new URL(config.endSessionEndpoint);
    url.searchParams.set("client_id", config.clientId);
    url.searchParams.set("post_logout_redirect_uri", config.redirectUri);
    window.location.href = url.toString();
  } else {
    window.location.href = config?.redirectUri || "/";
  }
}

/**
 * CENTRALIZED FETCH WRAPPER:
 * Handles Bearer token injection, automatic expiration check, and 401-driven refresh.
 * All authenticated requests in this app should route through here.
 */
async function fetchWithAuth(url, options = {}) {
  // Refresh if locally expired
  await handleTokenRefresh(false);

  // Enforce Authorization header (caller cannot accidentally override it)
  const authHeaders = {
    ...(options.headers || {}),
    Authorization: `Bearer ${memoryStore.access_token}`,
  };

  let response = await fetch(url, { ...options, headers: authHeaders });

  if (response.status === 401) {
    // Force refresh on 401 (token might be revoked server-side)
    await handleTokenRefresh(true);
    const retryHeaders = {
      ...(options.headers || {}),
      Authorization: `Bearer ${memoryStore.access_token}`,
    };
    response = await fetch(url, { ...options, headers: retryHeaders });
  }

  return response;
}

/**
 * Public API
 */
export {
  currentToken,
  initSSOConfig,
  initializeAuth,
  redirectToAuthProvider,
  exchangeCodeForToken,
  getUserData,
  fetchWithAuth,
  isAuthenticated,
  logout,
};
