import { CanActivateFn, Router } from '@angular/router';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';

export const moderatorGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const platformId = inject(PLATFORM_ID);
  const router = inject(Router);

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  await authService.waitUntilAuthReady();

  if (!authService.isAuthenticated()) {
    authService.login()
    return false;
  }

  const banStatus = await firstValueFrom(authService.loadCurrentBanStatus());

  if (banStatus.banned) {
    return router.createUrlTree(['/account-banned']);
  }

  if (authService.isModerator()) {
    return true;
  }

  return router.createUrlTree(['/landing']);
};
