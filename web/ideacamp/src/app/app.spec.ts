import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { OAuthService } from 'angular-oauth2-oidc';

import { App } from './app';
import { AuthService } from './feature/auth/auth.service';
import { ProjectInvitationService } from './feature/my-projects/services/project-invitation.service';

describe('App', () => {
  const events$ = new Subject<void>();
  const oauthServiceMock = {
    events: events$.asObservable(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: OAuthService, useValue: oauthServiceMock },
        {
          provide: AuthService,
          useValue: {
            waitUntilAuthReady: () => Promise.resolve(),
            isLoggedIn: () => false,
            username: () => '',
            user: () => null,
            login: () => undefined,
            logout: () => undefined,
          },
        },
        {
          provide: ProjectInvitationService,
          useValue: { getInvitations: () => of([]) },
        },
      ],
      imports: [App],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the shell layout', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-header')).not.toBeNull();
    expect(compiled.querySelector('app-sidebar')).not.toBeNull();
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });
});
