import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { OAuthService } from 'angular-oauth2-oidc';

import { HeaderComponent } from './header.component';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;
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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{ provide: OAuthService, useValue: oauthServiceMock }],
      imports: [HeaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
