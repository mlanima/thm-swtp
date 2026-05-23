import { Injectable, signal, WritableSignal, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
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

  constructor(private oauthService: OAuthService) {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const authConfig: AuthConfig = {
      issuer: (environment as any).issuer,
      redirectUri: `${window.location.origin}/success`,
      postLogoutRedirectUri: `${window.location.origin}/impressum`,
      clientId: (environment as any).clientId,
      responseType: 'code',
      scope: (environment as any).scope ?? 'openid profile email',
    };

    this.oauthService.configure(authConfig);

    // Try to restore login state from URL/storage
    this.oauthService.loadDiscoveryDocumentAndTryLogin().then(() => {
      this.updateState();
    });

    // OAuth events may run outside Angular's zone — update signals on every event
    this.oauthService.events.subscribe(() => this.updateState());
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
    this.oauthService.initCodeFlow();
  }

  logout(): void {
    this.oauthService.logOut();
  }

  // Registration flow removed from the SPA. Registration should be handled directly in Keycloak or via a dedicated registration route.

  getAccessToken(): string | null {
    return this.oauthService.getAccessToken() ?? null;
  }
}
