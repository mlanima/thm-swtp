# Keycloak Integration — Backend & Frontend

Dieses Dokument beschreibt, wie Backend (Spring Boot) und Frontend (Angular) mit Keycloak als Identity Provider zusammenarbeiten. Der Code im Branch `demo/keycloak-integration` dient als Referenz.

---

## Architektur

```
Browser / Client
     │
     ├─► Keycloak (auth.swtp-ss26.de)
     │     └─ stellt JWT aus
     │
     └─► Spring Boot Backend (Port 8080)
           └─ validiert JWT, liest Rollen
```

Das Backend stellt **keine eigenen Tokens aus** und kennt keine Passwörter. Es validiert ausschließlich JWTs von Keycloak.

---

## Backend (Spring Boot)

### Dependencies (`pom.xml`)

```xml
<!-- OAuth2 Resource Server -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>

<!-- SQLite Dialect (Hibernate Community) -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-community-dialects</artifactId>
    <version>${hibernate.version}</version>
</dependency>
```

### `application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:sqlite:./dev.db
    driver-class-name: org.sqlite.JDBC

  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: update

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.swtp-ss26.de/realms/swtp
```

Spring holt sich den Public Key automatisch von Keycloak (`/realms/swtp/.well-known/openid-configuration`) und validiert damit Signatur und Ablaufzeit jedes eingehenden Tokens.

### `SecurityConfig`

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakJwtConverter keycloakJwtConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter)));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

- **Stateless** — keine Sessions, kein CSRF
- **CORS** — erlaubt Requests von `localhost:4200` (Angular Dev-Server); für Produktion durch die echte Frontend-Domain ersetzen
- `/api/public/**` — ohne Token erreichbar
- Alles andere — gültiger JWT erforderlich

### `KeycloakJwtConverter`

Keycloak packt Rollen in `realm_access.roles` — Spring Security erwartet sie als `GrantedAuthority`. Diese Klasse übersetzt:

```java
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, extractRoles(jwt), jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();

        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) return List.of();

        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .map(GrantedAuthority.class::cast)
            .toList();
    }
}
```

### Rollen in Endpoints nutzen

```java
@GetMapping("/hello")
public Map<String, Object> securedHello(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
    return Map.of(
        "message", "Hello, " + jwt.getClaimAsString("preferred_username") + "!",
        "userId", jwt.getSubject(),  // Keycloak UUID — auch PK in der App-DB
        "roles", authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList()
    );
}
```

Rollen für Endpoint-Absicherung:

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/**").hasRole("USER")
```

### Testen

Token holen und direkt verwenden:

```bash
TOKEN=*** -s -X POST \
  https://auth.swtp-ss26.de/realms/swtp/protocol/openid-connect/token \
  -d "client_id=swtp-frontend" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=swtp26" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# Öffentlicher Endpoint
curl http://localhost:8080/api/public/hello

# Gesicherter Endpoint
curl http://localhost:8080/api/hello -H "Authorization: Bearer ***
```

> **Hinweis:** `grant_type=password` (Direct Access Grants) ist nur für Tests aktiviert. In Produktion verwendet Angular den Authorization Code Flow — kein Passwort im Request.

---

## Frontend (Angular)

### Setup

```bash
ng new frontend --routing=true --style=css --ssr=false --skip-git=true
cd frontend
npm install angular-oauth2-oidc
```

### `app.config.ts`

```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
    provideOAuthClient(),
  ]
};
```

### `AuthService`

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {

  isLoggedIn = signal(false);

  constructor(private oauthService: OAuthService) {
    this.oauthService.configure({
      issuer: 'https://auth.swtp-ss26.de/realms/swtp',
      redirectUri: window.location.origin,
      clientId: 'swtp-frontend',
      responseType: 'code',
      scope: 'openid profile email',
    });

    this.oauthService.loadDiscoveryDocumentAndTryLogin().then(() => {
      this.isLoggedIn.set(this.oauthService.hasValidAccessToken());
    });

    // OAuthService-Events laufen außerhalb von Angulars Zone —
    // Signal statt getter nötig damit Change Detection greift
    this.oauthService.events.subscribe(() => {
      this.isLoggedIn.set(this.oauthService.hasValidAccessToken());
    });
  }

  login(): void { this.oauthService.initCodeFlow(); }
  logout(): void { this.oauthService.logOut(); }

  get username(): string {
    const claims = this.oauthService.getIdentityClaims() as Record<string, string>;
    return claims?.['preferred_username'] ?? '';
  }

  get accessToken(): string {
    return this.oauthService.getAccessToken();
  }
}
```

> **Wichtig:** `isLoggedIn` muss ein Signal sein, kein getter. Die OAuth-Library arbeitet außerhalb von Angulars Zone — ein normaler getter löst keine Change Detection aus, der Status würde erst beim nächsten User-Interaction aktualisiert.

### Token an Backend schicken

```typescript
callSecuredApi(): void {
  const headers = new HttpHeaders({ Authorization: `Bearer ${this.auth.accessToken}` });
  this.http.get<object>('http://localhost:8080/api/hello', { headers })
    .subscribe(res => this.apiResponse.set(JSON.stringify(res, null, 2)));
}
```

> API-Responses ebenfalls als Signal (`signal('')`) speichern, nicht als plain property — aus demselben Grund.

---

## Keycloak-Setup

Voraussetzungen für diesen Stack: siehe [`infra/keycloak/`](../infra/keycloak/) und den KB-Artikel **Keycloak — Setup & Konfiguration**.

Realm `swtp`, Clients und Rollen müssen angelegt sein — Details im Setup-Guide.