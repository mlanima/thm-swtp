import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { GithubConnectionService } from '../../services/github-connection.service';

type CallbackState = 'loading' | 'success' | 'denied' | 'error';

const RETURN_TO_KEY = 'github_return_to';
const DEFAULT_RETURN_TO = '/settings';

@Component({
  selector: 'app-github-callback',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './github-callback.html',
})
export class GithubCallback implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly githubConnectionService = inject(GithubConnectionService);
  private readonly translate = inject(TranslateService);

  readonly state = signal<CallbackState>('loading');
  readonly errorMessage = signal('');

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const params = this.route.snapshot.queryParamMap;
    const error = params.get('error');
    const code = params.get('code');
    const state = params.get('state');

    if (error) {
      this.state.set('denied');
      return;
    }

    if (!code || !state) {
      this.state.set('error');
      this.errorMessage.set(this.translate.instant('GITHUB.CALLBACK.ERROR_DESCRIPTION'));
      return;
    }

    this.githubConnectionService.completeCallback(code, state).subscribe({
      next: () => {
        this.state.set('success');
        this.redirectBack();
      },
      error: () => {
        this.state.set('error');
        this.errorMessage.set(this.translate.instant('GITHUB.CALLBACK.ERROR_DESCRIPTION'));
      },
    });
  }

  goToSettings(): void {
    void this.router.navigateByUrl(DEFAULT_RETURN_TO);
  }

  private redirectBack(): void {
    const returnTo = sessionStorage.getItem(RETURN_TO_KEY) ?? DEFAULT_RETURN_TO;
    sessionStorage.removeItem(RETURN_TO_KEY);
    void this.router.navigateByUrl(returnTo);
  }
}
