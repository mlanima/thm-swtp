import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { OAuthService } from 'angular-oauth2-oidc';
import { vi } from 'vitest';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  const events$ = new Subject<void>();

  const oauthServiceMock = {
    // Minimal stub implementing the methods AuthService calls.
    events: events$.asObservable(),
    configure: vi.fn(),
    loadDiscoveryDocumentAndTryLogin: () => Promise.resolve(),
    hasValidAccessToken: () => false,
    getIdentityClaims: () => null,
    initCodeFlow: vi.fn(),
    getAccessToken: () => null,
    logOut: vi.fn(),
  } as unknown as Partial<OAuthService>;

  beforeEach(() => {
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
});
