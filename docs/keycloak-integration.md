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
}
```

- **Stateless** — keine Sessions, kein CSRF
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

## Frontend (Angular) — Ausblick

> Implementierung folgt im selben Branch.

Angular übernimmt den **Authorization Code Flow**:

1. User klickt "Login" → Redirect zu Keycloak
2. User gibt Credentials auf der Keycloak-Seite ein
3. Keycloak redirectet zurück mit Auth Code
4. Angular tauscht Code gegen Token
5. Token wird bei jedem API-Request als `Authorization: Bearer` Header mitgeschickt

Empfohlene Library: [`angular-oauth2-oidc`](https://github.com/manfredsteyer/angular-oauth2-oidc)

Keycloak-Config im Frontend:

```typescript
const authConfig: AuthConfig = {
  issuer: 'https://auth.swtp-ss26.de/realms/swtp',
  redirectUri: window.location.origin,
  clientId: 'swtp-frontend',
  responseType: 'code',
  scope: 'openid profile email',
};
```

---

## Keycloak-Setup

Voraussetzungen für diesen Stack: siehe [`infra/keycloak/`](../infra/keycloak/) und den KB-Artikel **Keycloak — Setup & Konfiguration**.

Realm `swtp`, Clients und Rollen müssen angelegt sein — Details im Setup-Guide.