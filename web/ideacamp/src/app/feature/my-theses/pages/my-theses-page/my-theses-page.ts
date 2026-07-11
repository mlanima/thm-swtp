import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MyThesesService } from '../../services/my-theses.service';
import { ThesisNotificationService } from '../../services/thesis-notification.service';
import { ThesisResponse } from '../../../../models/thesis.model';
import { AuthService } from '../../../auth/auth.service';
import { UserProfileService } from '../../../../services/user-profile.service';
import { ThesisList } from '../../components/thesis-list/thesis-list';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { SidebarService } from '../../../../shared/sidebar/sidebar.service';

@Component({
  selector: 'app-my-theses-page',
  standalone: true,
  imports: [RouterLink, ThesisList, TranslatePipe],
  templateUrl: './my-theses-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyThesesPage implements OnInit {
  private readonly myThesesService = inject(MyThesesService);
  private readonly thesisNotificationService = inject(ThesisNotificationService);
  private readonly authService = inject(AuthService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly translateService = inject(TranslateService);
  private readonly sidebarService = inject(SidebarService);
  readonly theses = signal<ThesisResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');
  readonly isProfessor = signal(false);

  ngOnInit(): void {
    this.authService.waitUntilAuthReady().then(() => {
      this.loadTheses();
      this.loadIsProfessor();
      this.markThesisNotificationsAsRead();
    });
  }

  private markThesisNotificationsAsRead(): void {
    this.thesisNotificationService.markAllRead().subscribe({
      next: () => this.sidebarService.unreadThesisNotificationsCount.set(0),
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