import { Component, inject, OnInit } from '@angular/core';
import { AuthService } from '../../../auth/auth.service';
import { TranslatePipe } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';


@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './landing-page.html',
})
export class LandingPage implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  auth = inject(AuthService);
  async ngOnInit(): Promise<void> {
    await this.authService.waitUntilAuthReady();

    if (!this.authService.isAuthenticated()) {
      return;
    }

    this.authService
      .loadCurrentBanStatus()
      .pipe(catchError(() => of({ banned: false, banReason: null, bannedAt: null })))
      .subscribe((status) => {
        if (status.banned) {
          this.router.navigateByUrl('/account-banned');
        }
      });
  }

  login(): void {
    this.authService.login();
  }
}
