# Frontend (web/ideacamp/)

Angular 21 app (SSR via Angular Universal/Express), TypeScript.

- Auth: `angular-oauth2-oidc` (Keycloak).
- Validation: `zod`.
- Icons: `primeicons`.
- Linting: ESLint with `@angular-eslint` + `typescript-eslint` (see `eslint.config.js`).

## Review focus

- Consistency with Angular/ESLint conventions already configured.
- Component structure, RxJS usage (avoid leaks/unhandled subscriptions), type safety.
- Auth/session handling correctness (OIDC flows).
- Avoid suggesting unrelated framework migrations or large refactors.
