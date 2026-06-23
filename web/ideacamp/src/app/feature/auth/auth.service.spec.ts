import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { OAuthService } from 'angular-oauth2-oidc';
import { vi } from 'vitest';

import { AuthService } from './auth.service';

function b64url(s: string): string {
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function makeToken(payload: Record<string, unknown>): string {
  return `header.${b64url(JSON.stringify(payload))}.signature`;
}

function tick(): Promise<void> {
  return new Promise((r) => setTimeout(r, 100));
}

describe('AuthService', () => {
  let service: AuthService;
  const events$ = new Subject<void>();

  const oauthServiceMock = {
    events: events$.asObservable(),
    configure: vi.fn(),
    loadDiscoveryDocumentAndTryLogin: () => Promise.resolve(),
    hasValidAccessToken: vi.fn(() => false),
    getIdentityClaims: vi.fn(() => null),
    initCodeFlow: vi.fn(),
    getAccessToken: vi.fn(() => null),
    logOut: vi.fn(),
  } as unknown as Partial<OAuthService>;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: OAuthService, useValue: oauthServiceMock },
      ],
    });
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('isModerator', () => {
    it('should be false when not authenticated', () => {
      expect(service.isModerator()).toBe(false);
    });

    it('should be false when token has no realm_access', async () => {
      vi.mocked(oauthServiceMock.hasValidAccessToken!).mockReturnValue(true);
      vi.mocked(oauthServiceMock.getAccessToken!).mockReturnValue(
        makeToken({ sub: 'abc' }),
      );
      events$.next();
      await tick();
      expect(service.isModerator()).toBe(false);
    });

    it('should be false when realm_access.roles is empty', async () => {
      vi.mocked(oauthServiceMock.hasValidAccessToken!).mockReturnValue(true);
      vi.mocked(oauthServiceMock.getAccessToken!).mockReturnValue(
        makeToken({ sub: 'abc', realm_access: { roles: [] } }),
      );
      events$.next();
      await tick();
      expect(service.isModerator()).toBe(false);
    });

    it('should be false when MODERATOR is not in roles', async () => {
      vi.mocked(oauthServiceMock.hasValidAccessToken!).mockReturnValue(true);
      vi.mocked(oauthServiceMock.getAccessToken!).mockReturnValue(
        makeToken({ sub: 'abc', realm_access: { roles: ['USER'] } }),
      );
      events$.next();
      await tick();
      expect(service.isModerator()).toBe(false);
    });

    it('should be true when MODERATOR is in realm_access.roles', async () => {
      vi.mocked(oauthServiceMock.hasValidAccessToken!).mockReturnValue(true);
      vi.mocked(oauthServiceMock.getAccessToken!).mockReturnValue(
        makeToken({ sub: 'abc', realm_access: { roles: ['MODERATOR'] } }),
      );
      events$.next();
      await tick();
      expect(service.isModerator()).toBe(true);
    });

    it('should be false when token contains base64url characters', async () => {
      const payload = { sub: 'abc', realm_access: { roles: ['USER'] } };
      vi.mocked(oauthServiceMock.hasValidAccessToken!).mockReturnValue(true);
      vi.mocked(oauthServiceMock.getAccessToken!).mockReturnValue(
        `header.${b64url(JSON.stringify(payload))}.signature`,
      );
      events$.next();
      await tick();
      expect(service.isModerator()).toBe(false);
    });

    it('should be false when access token is malformed', async () => {
      vi.mocked(oauthServiceMock.hasValidAccessToken!).mockReturnValue(true);
      vi.mocked(oauthServiceMock.getAccessToken!).mockReturnValue('not-a-jwt');
      events$.next();
      await tick();
      expect(service.isModerator()).toBe(false);
    });
  });
});
