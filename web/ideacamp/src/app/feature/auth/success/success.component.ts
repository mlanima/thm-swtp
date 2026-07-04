import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { UserProfileService } from '../../../services/user-profile.service';
import { TranslatePipe } from '@ngx-translate/core';
import { isModeratorReadableRoute } from '../moderator-readable-routes';

@Component({
  selector: 'app-success',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './success.component.html',
})
export class SuccessComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    await this.authService.waitUntilAuthReady();

    if (!this.authService.isAuthenticated()) {
      await this.router.navigateByUrl('/landing');
      return;
    }

    const redirectUrl = sessionStorage.getItem('postLoginRedirectUrl');

    if (this.authService.isModerator()) {
      if (redirectUrl && isModeratorReadableRoute(redirectUrl)) {
        sessionStorage.removeItem('postLoginRedirectUrl');
        await this.router.navigateByUrl(redirectUrl);
        return;
      }

      sessionStorage.removeItem('postLoginRedirectUrl');
      await this.router.navigateByUrl('/moderator');
      return;
    }

    if (redirectUrl) {
      sessionStorage.removeItem('postLoginRedirectUrl');
      await this.router.navigateByUrl(redirectUrl);
      return;
    }

    sessionStorage.removeItem('postLoginRedirectUrl');
    this.userProfileService.getMyProfile().subscribe();
  }
}
