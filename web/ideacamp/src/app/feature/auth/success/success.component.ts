import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { UserProfileService } from '../../../services/user-profile.service';

@Component({
  selector: 'app-success',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './success.component.html',
})
export class SuccessComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    await this.authService.waitUntilAuthReady();

    if (!this.authService.isAuthenticated()) {
      await this.router.navigateByUrl('/impressum');
      return;
    }

    this.userProfileService.getMyProfile().subscribe();
  }
}
