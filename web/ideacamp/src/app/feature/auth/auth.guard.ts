import { CanActivateFn } from '@angular/router';
import {inject, PLATFORM_ID} from '@angular/core';
import {isPlatformBrowser} from '@angular/common'
import {AuthService} from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const platformId = inject(PLATFORM_ID);

  if(!isPlatformBrowser(platformId)) {
    return true;
  }

  if (authService.isAuthenticated()) {
    return true;
  }
  authService.login();
  return false;
};
