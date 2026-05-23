import { Injectable, signal, WritableSignal, inject, PLATFORM_ID, ApplicationRef } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { filter, take } from 'rxjs';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../enviroments/enviroment.dev';
import { User } from '../../shared/types/user.type';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  readonly isLoggedIn: WritableSignal<boolean> = signal(false);
  readonly user: WritableSignal<User | null> = signal(null);
  readonly username = signal('');
  private readonly platformId = inject(PLATFORM_ID);
  private readonly appRef = inject(ApplicationRef);
  private initPromise: Promise<void> | null = null;
  private readonly redirectUri = isPlatformBrowser(this.platformId)
    ? `${window.location.origin}/success`
    : '';

  constructor(private oauthService: OAuthService) {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const authConfig: AuthConfig = {
      issuer: (environment as any).issuer,
      redirectUri: this.redirectUri,
      postLogoutRedirectUri: `${window.location.origin}/impressum`,
      clientId: (environment as any).clientId,
      responseType: 'code',
      scope: (environment as any).scope ?? 'openid profile email',
    };

    this.oauthService.configure(authConfig);

    // OAuth events may run outside Angular's zone — update signals on every event
    this.oauthService.events.subscribe(() => this.updateState());

    // Start discovery only after initial stabilization to avoid hydration timeout warnings.
    this.appRef.isStable
      .pipe(filter(Boolean), take(1))
      .subscribe(() => {
        this.startAuthBootstrap();
      });
  }

  private startAuthBootstrap(): Promise<void> {
    if (this.initPromise) {
      return this.initPromise;
    }

    this.initPromise = this.oauthService.loadDiscoveryDocumentAndTryLogin()
      .then(() => this.updateState())
      .catch(() => this.updateState());

    return this.initPromise;
  }

  private updateState(): void {
    const hasToken = this.oauthService.hasValidAccessToken();
    this.isLoggedIn.set(hasToken);

    if (hasToken) {
      const claims = this.oauthService.getIdentityClaims() as Record<string, any> | null;
      const username = claims?.['preferred_username'] ?? '';
      this.username.set(username);
      this.user.set({ username });
    } else {
      this.username.set('');
      this.user.set(null);
    }
  }

  login(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const startLogin = () => {
      try {
        this.oauthService.initCodeFlow();
      } catch {
        this.redirectToKeycloakLogin();
      }
    };

    // Ensure discovery/login endpoints are available before starting code flow.
    const init = this.startAuthBootstrap();

    let started = false;
    const startOnce = () => {
      if (started) {
        return;
      }

      started = true;
      startLogin();
    };

    // If discovery hangs, still allow user to continue with direct auth redirect.
    setTimeout(startOnce, 1500);
    init.finally(startOnce);
  }

  private redirectToKeycloakLogin(): void {
    const issuer = ((environment as any).issuer ?? '').replace(/\/$/, '');
    const clientId = (environment as any).clientId ?? '';
    const scope = (environment as any).scope ?? 'openid profile email';

    window.location.href =
      `${issuer}/protocol/openid-connect/auth?client_id=${encodeURIComponent(clientId)}` +
      `&redirect_uri=${encodeURIComponent(this.redirectUri)}` +
      `&response_type=code` +
      `&scope=${encodeURIComponent(scope)}`;
  }

  logout(): void {
    this.oauthService.logOut();
  }

  // Registration flow removed from the SPA. Registration should be handled directly in Keycloak or via a dedicated registration route.

  getAccessToken(): string | null {
    return this.oauthService.getAccessToken() ?? null;
  }
}
