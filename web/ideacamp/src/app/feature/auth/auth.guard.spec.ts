import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard } from './auth.guard';
import { vi } from 'vitest';

const mockRoute = {} as ActivatedRouteSnapshot;
const mockState = {} as RouterStateSnapshot;

describe('authGuard', () => {
  let router: Router;

  const authServiceMock = {
    waitUntilAuthReady: vi.fn(() => Promise.resolve()),
    isLoggingOut: vi.fn(() => false),
    isAuthenticated: vi.fn(() => false),
    isModerator: vi.fn(() => false),
    login: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    authServiceMock.isAuthenticated.mockReturnValue(false);
    authServiceMock.isModerator.mockReturnValue(false);
    authServiceMock.isLoggingOut.mockReturnValue(false);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    });
    router = TestBed.inject(Router);
  });

  it('should allow regular authenticated users', async () => {
    authServiceMock.isAuthenticated.mockReturnValue(true);
    authServiceMock.isModerator.mockReturnValue(false);
    authServiceMock.isLoggingOut.mockReturnValue(false);

    const result = await TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
    expect(result).toBe(true);
  });

  it('should redirect moderators to /moderator', async () => {
    authServiceMock.isAuthenticated.mockReturnValue(true);
    authServiceMock.isModerator.mockReturnValue(true);
    authServiceMock.isLoggingOut.mockReturnValue(false);

    const result = await TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
    expect(result).toEqual(router.parseUrl('/moderator'));
  });

  it('should redirect to /impressum when logging out', async () => {
    authServiceMock.isAuthenticated.mockReturnValue(false);
    authServiceMock.isModerator.mockReturnValue(false);
    authServiceMock.isLoggingOut.mockReturnValue(true);

    const result = await TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
    expect(result).toEqual(router.parseUrl('/landing'));
  });

  it('should call login when not authenticated', async () => {
    authServiceMock.isAuthenticated.mockReturnValue(false);
    authServiceMock.isModerator.mockReturnValue(false);
    authServiceMock.isLoggingOut.mockReturnValue(false);

    const result = await TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
    expect(authServiceMock.login).toHaveBeenCalled();
    expect(result).toBe(false);
  });
});
