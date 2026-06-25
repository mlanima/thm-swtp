import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, firstValueFrom, of } from 'rxjs';
import { AuthService } from './auth.service';

export const bannedAccountGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const platformId = inject(PLATFORM_ID);
  const router = inject(Router);

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  await authService.waitUntilAuthReady();

  if (!authService.isAuthenticated()) {
    authService.login();
    return false;
  }

  const banStatus = await firstValueFrom(
    authService
      .loadCurrentBanStatus()
      .pipe(catchError(() => of({ banned: false, banReason: null, bannedAt: null }))),
  );

  if (banStatus.banned) {
    return true;
  }

  if (authService.isModerator()) {
    return router.createUrlTree(['/moderator']);
  }

  return router.createUrlTree(['/landing']);
};
