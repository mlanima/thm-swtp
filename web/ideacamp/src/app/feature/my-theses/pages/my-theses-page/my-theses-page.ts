import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MyThesesService } from '../../services/my-theses.service';
import { ThesisResponse } from '../../../../models/thesis.model';
import { AuthService } from '../../../auth/auth.service';
import { UserProfileService } from '../../../../services/user-profile.service';
import { ThesisList } from '../../components/thesis-list/thesis-list';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-my-theses-page',
  standalone: true,
  imports: [RouterLink, ThesisList, TranslatePipe],
  templateUrl: './my-theses-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyThesesPage implements OnInit {
  private readonly myThesesService = inject(MyThesesService);
  private readonly authService = inject(AuthService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly translateService = inject(TranslateService);
  readonly theses = signal<ThesisResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');
  readonly isProfessor = signal(false);

  ngOnInit(): void {
    this.authService.waitUntilAuthReady().then(() => {
      this.loadTheses();
      this.loadIsProfessor();
    });
  }

  private loadIsProfessor(): void {
    this.userProfileService.getMyProfile().subscribe({
      next: (profile) => this.isProfessor.set(profile.isProfessor),
    });
  }

  private loadTheses(): void {
    const username = this.authService.username();

    if (!username) {
      this.errorMessage.set(this.translateService.instant('MYTHESES.ERROR_LOAD_USERNAME'));
      this.isLoading.set(false);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.myThesesService.getMyTheses(username).subscribe({
      next: (theses) => {
        this.theses.set(theses);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('MYTHESES.ERROR_LOAD_THESES'));
        this.isLoading.set(false);
      },
    });
  }
}