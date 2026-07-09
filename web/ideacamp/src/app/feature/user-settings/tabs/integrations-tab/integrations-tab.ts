import { Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { GithubConnectionService } from '../../../github/services/github-connection.service';
import { GithubConnectionStatusModel } from '../../../../models/github-connection.model';

const RETURN_TO_KEY = 'github_return_to';

@Component({
  selector: 'app-integrations-tab',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './integrations-tab.html',
})
export class IntegrationsTab implements OnInit {
  private readonly githubConnectionService = inject(GithubConnectionService);
  private readonly translate = inject(TranslateService);

  readonly isLoading = signal(true);
  readonly status = signal<GithubConnectionStatusModel | null>(null);
  readonly isConnecting = signal(false);
  readonly isDisconnecting = signal(false);
  readonly errorMessage = signal('');
  readonly showDisconnectModal = signal(false);

  ngOnInit(): void {
    this.githubConnectionService.getStatus().subscribe({
      next: (status) => {
        this.status.set(status);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('GITHUB.ERROR_LOAD'));
        this.isLoading.set(false);
      },
    });
  }

  connect(): void {
    this.isConnecting.set(true);
    this.errorMessage.set('');

    this.githubConnectionService.getAuthorizeUrl().subscribe({
      next: (url) => {
        sessionStorage.setItem(RETURN_TO_KEY, '/settings');
        window.location.href = url;
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('GITHUB.ERROR_CONNECT'));
        this.isConnecting.set(false);
      },
    });
  }

  openDisconnectModal(): void {
    this.showDisconnectModal.set(true);
  }

  closeDisconnectModal(): void {
    this.showDisconnectModal.set(false);
  }

  confirmDisconnect(): void {
    this.isDisconnecting.set(true);

    this.githubConnectionService.disconnect().subscribe({
      next: () => {
        const previous = this.status();
        this.status.set({
          enabled: previous?.enabled ?? true,
          connected: false,
          githubLogin: null,
          avatarUrl: null,
          status: null,
        });
        this.isDisconnecting.set(false);
        this.showDisconnectModal.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('GITHUB.ERROR_DISCONNECT'));
        this.isDisconnecting.set(false);
        this.showDisconnectModal.set(false);
      },
    });
  }
}
