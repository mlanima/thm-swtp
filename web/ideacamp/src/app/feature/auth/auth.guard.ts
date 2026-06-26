import { CanActivateFn, Router } from '@angular/router';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common'
import { AuthService } from './auth.service';
import { firstValueFrom, catchError, of } from 'rxjs';

export const authGuard: CanActivateFn = async () => {
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

    if (authService.isModerator()) {
      return router.createUrlTree(['/moderator']);
    }
    return true;
  }

  authService.login()
  return false;
};
