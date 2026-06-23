import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { moderatorGuard } from './moderator.guard';
import { vi } from 'vitest';

const mockRoute = {} as ActivatedRouteSnapshot;
const mockState = {} as RouterStateSnapshot;

describe('moderatorGuard', () => {
  let router: Router;

  const authServiceMock = {
    waitUntilAuthReady: vi.fn(() => Promise.resolve()),
    isAuthenticated: vi.fn(() => false),
    isModerator: vi.fn(() => false),
    login: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    });
    router = TestBed.inject(Router);
  });

  it('should allow moderators through', async () => {
    authServiceMock.isAuthenticated.mockReturnValue(true);
    authServiceMock.isModerator.mockReturnValue(true);

    const result = await TestBed.runInInjectionContext(() => moderatorGuard(mockRoute, mockState));
    expect(result).toBe(true);
  });

  it('should redirect non-moderator authenticated users to /landing', async () => {
    authServiceMock.isAuthenticated.mockReturnValue(true);
    authServiceMock.isModerator.mockReturnValue(false);

    const result = await TestBed.runInInjectionContext(() => moderatorGuard(mockRoute, mockState));
    expect(result).toEqual(router.parseUrl('/landing'));
  });

  it('should call login when not authenticated', async () => {
    authServiceMock.isAuthenticated.mockReturnValue(false);

    const result = await TestBed.runInInjectionContext(() => moderatorGuard(mockRoute, mockState));
    expect(authServiceMock.login).toHaveBeenCalled();
    expect(result).toBe(false);
  });
});
