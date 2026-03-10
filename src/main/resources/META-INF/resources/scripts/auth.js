class AuthService {
  initialize() {}

  login() {
    window.location.assign("/auth/login");
  }

  logout() {
    window.location.assign("/auth/logout");
  }
}

const authService = new AuthService();
export { authService };
