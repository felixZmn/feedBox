class AuthService {
    constructor() {
        this.tokens = null;
        this.refreshInProgress = false;
        this.refreshTimer = null;
    }

    /**
     * Public method to store tokens and schedule refresh
     * Used by callback handlers
     */
    storeTokensAndScheduleRefresh(tokens) {
        console.log("[auth] Public method: storing tokens");
        this._storeTokens(tokens);
        this._scheduleRefresh();
    }

    _storeTokens(tokens) {
        console.log(
            "[auth] Storing tokens - has refreshToken:",
            !!tokens.refreshToken,
        );
        localStorage.setItem("id_token", tokens.idToken);
        localStorage.setItem("access_token", tokens.accessToken);
        if (tokens.refreshToken) {
            localStorage.setItem("refresh_token", tokens.refreshToken);
        }
        // Store token metadata for expiration checking
        if (tokens.expiresIn) {
            const expiresAt = Date.now() + tokens.expiresIn * 1000;
            localStorage.setItem("access_token_expires_at", expiresAt);
            console.log("[auth] Token expires in", tokens.expiresIn, "seconds");
        }
    }

    _clearTokens() {
        localStorage.removeItem("id_token");
        localStorage.removeItem("access_token");
        localStorage.removeItem("refresh_token");
        localStorage.removeItem("access_token_expires_at");
    }

    /**
     * Extracts tokens from URL (query params or hash)
     * Handles OAuth callback formats:
     * - Query params: ?access_token=...&id_token=...&expires_in=...
     * - Hash: #access_token=...&id_token=...&expires_in=...
     */
    _extractTokensFromUrl() {
        // Try query parameters first
        const urlParams = new URLSearchParams(window.location.search);
        let accessToken = urlParams.get("access_token");
        let idToken = urlParams.get("id_token");
        let refreshToken = urlParams.get("refresh_token");
        let expiresIn = urlParams.get("expires_in");

        // If not in query params, try hash
        if (!accessToken && window.location.hash) {
            const hash = window.location.hash.substring(1);
            const hashParams = new URLSearchParams(hash);
            accessToken = hashParams.get("access_token");
            idToken = hashParams.get("id_token");
            refreshToken = hashParams.get("refresh_token");
            expiresIn = hashParams.get("expires_in");
        }

        if (!accessToken) {
            console.log(
                "[auth] No tokens found in URL. Search:",
                window.location.search,
                "Hash:",
                window.location.hash,
            );
            return null;
        }

        console.log("[auth] Tokens extracted from URL");
        return {
            accessToken,
            idToken,
            refreshToken,
            expiresIn: expiresIn ? parseInt(expiresIn, 10) : null,
        };
    }

    /**
     * Parses JWT token to get expiration time
     */
    _parseJwt(token) {
        try {
            const base64Url = token.split(".")[1];
            const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
            const jsonPayload = decodeURIComponent(
                atob(base64)
                    .split("")
                    .map((c) => `%${`00${c.charCodeAt(0).toString(16)}`.slice(-2)}`)
                    .join(""),
            );
            return JSON.parse(jsonPayload);
        } catch (error) {
            console.error("Failed to parse JWT:", error);
            return null;
        }
    }

    /**
     * Check if access token is expired (within 5 minute buffer)
     */
    _isAccessTokenExpired() {
        const expiresAt = localStorage.getItem("access_token_expires_at");
        if (!expiresAt) {
            // Try to get expiration from JWT payload
            const token = localStorage.getItem("access_token");
            if (!token) return true;
            const payload = this._parseJwt(token);
            if (!payload || !payload.exp) return false; // Can't determine, assume valid
            return payload.exp * 1000 < Date.now() + 5 * 60 * 1000; // 5 minute buffer
        }
        return parseInt(expiresAt, 10) < Date.now() + 5 * 60 * 1000; // 5 minute buffer
    }

    /**
     * Handles OAuth callback after redirect from auth provider
     */
    async handleCallback() {
        const tokens = this._extractTokensFromUrl();
        if (!tokens) {
            console.warn("No tokens found in callback URL");
            return false;
        }

        this._storeTokens(tokens);
        // Clean up URL (remove query params and hash)
        window.history.replaceState({}, document.title, window.location.pathname);
        return true;
    }

    /**
     * Refresh the access token using the refresh token
     * Returns true if refresh was successful, false otherwise
     */
    async refreshAccessToken() {
        const refreshToken = localStorage.getItem("refresh_token");

        // If no refresh token, we can't refresh - user needs to re-login
        if (!refreshToken) {
            console.warn("No refresh token available");
            return false;
        }

        // Prevent concurrent refresh requests
        if (this.refreshInProgress) {
            // Wait for the in-progress refresh to complete
            return new Promise((resolve) => {
                const checkRefresh = setInterval(() => {
                    if (!this.refreshInProgress) {
                        clearInterval(checkRefresh);
                        resolve(true);
                    }
                }, 100);
            });
        }

        this.refreshInProgress = true;
        try {
            const response = await fetch("/auth/refresh", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({refresh_token: refreshToken}),
            });

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    // Refresh token is invalid/expired, user needs to re-login
                    console.warn("Refresh token invalid, logout required");
                    this._clearTokens();
                    this.stopRefreshTimer();
                    return false;
                }
                throw new Error(`Refresh failed with status ${response.status}`);
            }

            const data = await response.json();
            this._storeTokens({
                accessToken: data.access_token,
                idToken: data.id_token,
                refreshToken: data.refresh_token || refreshToken, // Use new refresh token if provided
                expiresIn: data.expires_in,
            });

            // Reschedule refresh timer
            this._scheduleRefresh();
            return true;
        } catch (error) {
            console.error("Token refresh failed:", error);
            return false;
        } finally {
            this.refreshInProgress = false;
        }
    }

    /**
     * Schedule automatic token refresh (PWA-friendly)
     * Refreshes token 5 minutes before expiration
     */
    _scheduleRefresh() {
        this.stopRefreshTimer();

        const expiresAt = localStorage.getItem("access_token_expires_at");
        const token = localStorage.getItem("access_token");

        if (!expiresAt && !token) {
            return; // No token to refresh
        }

        let timeToRefresh;
        if (expiresAt) {
            // Use stored expiration with 5 minute buffer
            timeToRefresh = parseInt(expiresAt, 10) - Date.now() - 5 * 60 * 1000;
        } else {
            // Parse JWT for expiration
            const payload = this._parseJwt(token);
            if (!payload || !payload.exp) {
                return; // Can't determine expiration
            }
            timeToRefresh = payload.exp * 1000 - Date.now() - 5 * 60 * 1000;
        }

        if (timeToRefresh > 0) {
            this.refreshTimer = setTimeout(() => {
                this.refreshAccessToken();
            }, timeToRefresh);
        }
    }

    /**
     * Stop the refresh timer
     */
    stopRefreshTimer() {
        if (this.refreshTimer) {
            clearTimeout(this.refreshTimer);
            this.refreshTimer = null;
        }
    }

    /**
     * Initialize auth - check callback and setup refresh
     */
    async initialize() {
        console.log("[auth] Initializing auth service");

        // Always check for tokens in URL (query params or hash) first
        const tokens = this._extractTokensFromUrl();
        if (tokens) {
            // We found tokens in the URL - this is a callback from OAuth provider
            console.log("[auth] Processing OAuth callback, storing tokens");
            this._storeTokens(tokens);
            // Clean up URL
            window.history.replaceState({}, document.title, window.location.pathname);
            console.log("[auth] Tokens stored, redirecting to /");
            // Redirect to home after successful callback
            window.location.href = "/";
            return;
        }

        // Schedule token refresh if token exists
        const token = localStorage.getItem("access_token");
        if (token) {
            console.log("[auth] Token found in storage, scheduling refresh");
            this._scheduleRefresh();
        } else {
            console.log("[auth] No token found in storage");
        }
    }

    login() {
        this.stopRefreshTimer();
        window.location.href = "/auth/login";
    }

    logout() {
        this._clearTokens();
        this.stopRefreshTimer();
        window.location.href = "/auth/logout";
    }

    getAccessToken() {
        return localStorage.getItem("access_token");
    }

    getIdToken() {
        return localStorage.getItem("id_token");
    }

    /**
     * Get access token with automatic refresh if needed
     * This ensures the token is valid before use
     */
    async getValidAccessToken() {
        return this.getAccessToken();
    }
}

const authService = new AuthService();

// Auto-initialize on page load to catch OAuth callbacks
if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        authService.initialize();
    });
} else {
    // DOMContentLoaded already fired
    authService.initialize();
}

export {authService};
