import { DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-banned-account',
  standalone: true,
  imports: [DatePipe, TranslatePipe],
  templateUrl: './banned-account.html',
})
export class BannedAccount implements OnInit{
  private readonly authService = inject(AuthService);
  private readonly translateService = inject(TranslateService);
  private readonly router = inject(Router);

  banStatus = this.authService.currentBanStatus;

  ngOnInit(): void {
    this.authService.loadCurrentBanStatus().subscribe(status => {
      if (!status.banned) {
        this.router.navigateByUrl('/landing');
      }
    })
  }

  setLanguage(language: 'de' | 'en'): void {
    this.translateService.use(language);
  }

  logout(): void {
    this.authService.logout();
  }
}
