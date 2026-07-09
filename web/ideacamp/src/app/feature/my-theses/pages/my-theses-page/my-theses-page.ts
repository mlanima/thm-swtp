import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ThesisService } from '../../../thesis-site/thesis.service';
import { ThesisResponse } from '../../../../models/thesis.model';
import { AuthService } from '../../../auth/auth.service';
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
  private readonly thesisService = inject(ThesisService);
  private readonly authService = inject(AuthService);
  private readonly translateService = inject(TranslateService);
  readonly theses = signal<ThesisResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');

  ngOnInit(): void {
    this.authService.waitUntilAuthReady().then(() => {
      this.loadTheses();
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

    this.thesisService.getMyTheses(username).subscribe({
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