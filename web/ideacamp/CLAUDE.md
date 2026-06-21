# Frontend (web/ideacamp/)

Angular 21 app (SSR via `@angular/ssr` + Express), TypeScript.

- Auth: `angular-oauth2-oidc` (Keycloak).
- i18n: `@ngx-translate/core` + `@ngx-translate/http-loader`. Use `TranslateService.instant()`
  to resolve keys — never assign raw translation keys to template-bound properties.
- Validation: `zod`.
- Icons: `primeicons`.
- Linting: ESLint with `@angular-eslint` + `typescript-eslint` (see `eslint.config.js`).

## Review focus

- Consistency with Angular/ESLint conventions already configured.
- Component structure, RxJS usage (avoid leaks/unhandled subscriptions), type safety.
- Auth/session handling correctness (OIDC flows).
- Avoid suggesting unrelated framework migrations or large refactors.
