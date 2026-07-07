export const environment = {
  apiUrl: 'http://localhost:8080/api',
  keycloakUrl: 'https://auth.swtp-ss26.de',
  // OpenID Connect / Keycloak configuration used by AuthService
  issuer: 'https://auth.swtp-ss26.de/realms/swtp',
  clientId: 'swtp-frontend',
  scope: 'openid profile email',
};
