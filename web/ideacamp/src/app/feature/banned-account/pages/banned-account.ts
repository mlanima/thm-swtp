import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-banned-account',
  standalone: true,
  imports: [DatePipe, TranslatePipe],
  templateUrl: './banned-account.html',
})
export class BannedAccount implements OnInit{
  private readonly authService = inject(AuthService);
  private readonly translateService = inject(TranslateService);

  banStatus = this.authService.currentBanStatus;

  setLanguage(language: 'de' | 'en'): void {
    this.translateService.use(language);
  }

  logout(): void {
    this.authService.logout();
  }
}
