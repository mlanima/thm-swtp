import { CanActivateFn, Router } from '@angular/router';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common'
import { AuthService } from './auth.service';
import { firstValueFrom, catchError, of } from 'rxjs';
import { isModeratorReadableRoute } from './moderator-readable-routes';

export const authGuard: CanActivateFn = async (_route, state) => {
  const authService = inject(AuthService);
  const platformId = inject(PLATFORM_ID);
  const router = inject(Router);

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  await authService.waitUntilAuthReady();

  if (authService.isLoggingOut()) {
    return router.createUrlTree(['/landing']);
  }

  if (authService.isAuthenticated()) {
    const banStatus = await firstValueFrom(
      authService
        .loadCurrentBanStatus()
        .pipe(catchError(() => of({ banned: false, banReason: null, bannedAt: null }))),
    );

    if (banStatus.banned) {
      return router.createUrlTree(['/account-banned']);
    }

    if (isAuthCallbackRoute(state.url)) {
      return true;
    }

    if (authService.isModerator()) {
      if (isModeratorReadableRoute(state.url)){
        return true;
      }
      return router.createUrlTree(['/moderator']);
    }
    return true;
  }

  sessionStorage.setItem('postLoginRedirectUrl', state.url);
  authService.login();
  return false;
};

function isAuthCallbackRoute(url: string): boolean {
  return url.split('?')[0].split('#')[0] === '/success';
}
