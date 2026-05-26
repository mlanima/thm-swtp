import { Injectable, signal, WritableSignal, inject, PLATFORM_ID, ApplicationRef, NgZone } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { filter, take } from 'rxjs';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../enviroments/enviroment.dev';
import { User } from '../../shared/types/user.type';

type OAuthServiceBridge = Partial<Pick<
  OAuthService,
  | 'configure'
  | 'loadDiscoveryDocumentAndTryLogin'
  | 'hasValidAccessToken'
  | 'getIdentityClaims'
  | 'initCodeFlow'
  | 'logOut'
  | 'getAccessToken'
>> & {
  events?: { subscribe: (next: () => void) => unknown };
};

/**
 * Central authentication service for Keycloak/OIDC integration.
 *
 * Responsibilities:
 * - configure OAuth client for code flow
 * - bootstrap login state from discovery document + callback URL
 * - expose reactive auth state for UI components
 * - start login/logout flows
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  /** True when a valid access token is present. */
  readonly isLoggedIn: WritableSignal<boolean> = signal(false);

  /** Minimal authenticated user representation for UI components. */
  readonly user: WritableSignal<User | null> = signal(null);

  /** Convenience signal for showing the current username in the UI. */
  readonly username = signal('');
  private readonly platformId = inject(PLATFORM_ID);
  private readonly appRef = inject(ApplicationRef);
  private readonly ngZone = inject(NgZone);
  private updateStateTimer: ReturnType<typeof setTimeout> | null = null;
  // Resolve OAuthService inside the constructor so TestBed providers/mocks are
  // available when the service is instantiated in tests.
  private oauthService!: OAuthService;
  private initPromise: Promise<void> | null = null;
  private readonly redirectUri = isPlatformBrowser(this.platformId)
    ? `${window.location.origin}/success`
    : '';

  constructor() {
    // call inject() during construction to ensure TestBed provider overrides
    // are respected (field initializer inject() can run too early in tests).
    this.oauthService = inject(OAuthService);

    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const env = environment as { issuer?: string; clientId?: string; scope?: string };
    const authConfig: AuthConfig = {
      issuer: env.issuer ?? '',
      redirectUri: this.redirectUri,
      postLogoutRedirectUri: `${window.location.origin}/impressum`,
      clientId: env.clientId ?? '',
      responseType: 'code',
      scope: env.scope ?? 'openid profile email',
    };

    // Be defensive: in unit tests the provided OAuthService replacement may be a
    // lightweight stub; only call methods when available to avoid runtime errors.
    const oauthService = this.oauthService as OAuthServiceBridge;
    if (typeof oauthService.configure === 'function') {
      oauthService.configure(authConfig);
    }

    // OAuth events may run outside Angular's zone — update signals on every event
    const events = oauthService.events;
    if (events && typeof events.subscribe === 'function') {
      // events might be an Observable — subscribe to keep UI signals in sync.
      events.subscribe(() => void this.updateStateAfterTick());
    }

    // Start discovery only after initial stabilization to avoid hydration timeout warnings.
    this.appRef.isStable
      .pipe(filter(Boolean), take(1))
      .subscribe(() => {
        this.startAuthBootstrap();
      });
  }

  /**
   * Loads discovery document and attempts to restore login from callback/storage.
   * Returns the same promise while initialization is already running.
   */
  private startAuthBootstrap(): Promise<void> {
    if (this.initPromise) {
      return this.initPromise;
    }

    // If the OAuthService doesn't implement discovery in the test/mocked
    // instance, resolve immediately and update state to avoid hanging tests.
    const oauthService = this.oauthService as OAuthServiceBridge;
    if (typeof oauthService.loadDiscoveryDocumentAndTryLogin !== 'function') {
      this.initPromise = this.updateStateAfterTick();
      return this.initPromise;
    }

    this.initPromise = oauthService.loadDiscoveryDocumentAndTryLogin()
      .then(() => this.updateStateAfterTick())
      .catch(() => this.updateStateAfterTick());

    return this.initPromise!;
  }

  async waitUntilAuthReady(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    await this.startAuthBootstrap();
  }

  private updateStateAfterTick(): Promise<void> {
    return new Promise((resolve) => {
      if (this.updateStateTimer) {
        clearTimeout(this.updateStateTimer);
      }

      this.ngZone.runOutsideAngular(() => {
        this.updateStateTimer = setTimeout(() => {
          this.ngZone.run(() => {
            this.updateState();
            resolve();
          });
        }, 50);
      });
    });
  }

  private updateState(): void {
    const oauthService = this.oauthService as OAuthServiceBridge;
    const hasToken = oauthService.hasValidAccessToken?.() ?? false;

    if (this.isLoggedIn() !== hasToken) {
      this.isLoggedIn.set(hasToken);
    }

    if (hasToken) {
      const claims = oauthService.getIdentityClaims?.() as Record<string, unknown> | null;
      const preferred = claims?.['preferred_username'];
      const username = typeof preferred === 'string' ? preferred : '';
      if (this.username() !== username) {
        this.username.set(username);
      }
      const currentUser = this.user();
      if (currentUser?.username !== username) {
        this.user.set({ username });
      }
    } else {
      if (this.username() !== '') {
        this.username.set('');
      }
      if (this.user() !== null) {
        this.user.set(null);
      }
    }
  }

  /**
   * Starts Keycloak/OIDC login via authorization code flow.
   *
   * The method waits for auth bootstrap and includes a timeout fallback,
   * then redirects directly to the auth endpoint if initCodeFlow fails.
   */
  login(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const oauthService = this.oauthService as OAuthServiceBridge;

    const startLogin = () => {
      try {
        oauthService.initCodeFlow?.();
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

  /** Fallback redirect to the Keycloak authorization endpoint. */
  private redirectToKeycloakLogin(): void {
    const env = environment as { issuer?: string; clientId?: string; scope?: string };
    const issuer = ((env.issuer ?? '')).replace(/\/$/, '');
    const clientId = env.clientId ?? '';
    const scope = env.scope ?? 'openid profile email';

    window.location.href =
      `${issuer}/protocol/openid-connect/auth?client_id=${encodeURIComponent(clientId)}` +
      `&redirect_uri=${encodeURIComponent(this.redirectUri)}` +
      `&response_type=code` +
      `&scope=${encodeURIComponent(scope)}`;
  }

  /** Triggers OIDC logout and lets Keycloak redirect back to post logout URI. */
  logout(): void {
    const oauthService = this.oauthService as OAuthServiceBridge;
    oauthService.logOut?.();
  }

  // Registration flow removed from the SPA. Registration should be handled directly in Keycloak or via a dedicated registration route.

  /** Returns current access token for API calls (or null when not authenticated). */
  getAccessToken(): string | null {
    const oauthService = this.oauthService as OAuthServiceBridge;
    return oauthService.getAccessToken?.() ?? null;
  }

  /** Returns true if a valid access token exists. */
  isAuthenticated(): boolean {
    const oauthService = this.oauthService as OAuthServiceBridge;
    return oauthService.hasValidAccessToken?.() ?? false;
  }
}
