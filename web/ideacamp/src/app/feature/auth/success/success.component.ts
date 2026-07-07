import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { UserProfileService } from '../../../services/user-profile.service';
import { TranslatePipe } from '@ngx-translate/core';
import { Onboarding} from '../../../shared/onboarding/onboarding';

@Component({
  selector: 'app-success',
  standalone: true,
  imports: [TranslatePipe, Onboarding],
  templateUrl: './success.component.html',
})
export class SuccessComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly router = inject(Router);

  readonly showOnboarding = signal(false);
  private redirectUrl: string | null = null;

  async ngOnInit(): Promise<void> {
    await this.authService.waitUntilAuthReady();

    if (!this.authService.isAuthenticated()) {
      await this.router.navigateByUrl('/landing');
      return;
    }

    if (this.authService.isModerator()) {
      await this.router.navigateByUrl('/moderator');
      return;
    }

    this.redirectUrl = sessionStorage.getItem('postLoginRedirectUrl');
    sessionStorage.removeItem('postLoginRedirectUrl');

    if (this.redirectUrl) {
      await this.router.navigateByUrl(this.redirectUrl);
      return;
    }

    this.userProfileService.getMyProfile().subscribe({
      next: (profile) => {
        if (!profile.onboardingCompleted) {
          this.showOnboarding.set(true);
          return;
        }

        this.navigateAfterSuccess();
      },
      error: () => {
        this.navigateAfterSuccess();
      },
    });
  }

  completeOnboarding(): void {
    this.userProfileService.updateOnboardingCompleted(true).subscribe({
      next: () => {
        this.showOnboarding.set(false);
        this.navigateAfterSuccess();
      },
      error: () => {
        this.showOnboarding.set(false);
        this.navigateAfterSuccess();
      },
    });
  }

  private navigateAfterSuccess(): void {
    void this.router.navigateByUrl('/dashboard');
  }
}
