import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { OAuthService } from 'angular-oauth2-oidc';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  const events$ = new Subject<void>();

  const oauthServiceMock = {
    configure: () => {},
    loadDiscoveryDocumentAndTryLogin: () => Promise.resolve(true),
    hasValidAccessToken: () => false,
    getIdentityClaims: () => null,
    getAccessToken: () => '',
    initCodeFlow: () => {},
    logOut: () => {},
    events: events$.asObservable(),
  };

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
