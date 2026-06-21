import { Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProjectJoinRequestService, JoinRequestResponse } from '../../../../services/project-join-request.service';
import { ProjectSettingsStore } from '../../project-settings.store';

interface DisplayRequest {
  id: string;
  initials: string;
  username: string;
  message: string;
  requestDate: string;
}

@Component({
  selector: 'app-join-requests-tab',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './join-requests-tab.html',
})
export class JoinRequestsTab implements OnInit {
  private readonly store = inject(ProjectSettingsStore);
  private readonly joinRequestService = inject(ProjectJoinRequestService);
  private readonly translateService = inject(TranslateService);

  requests = signal<DisplayRequest[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    const projectId = this.store.project()?.id;
    if (!projectId) return;

    this.joinRequestService.getProjectRequests(projectId).subscribe({
      next: (data) => {
        this.requests.set(data.filter(r => r.status === 'PENDING').map(r => this.toDisplay(r)));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  approve(id: string): void {
    this.joinRequestService.acceptRequest(id).subscribe({
      next: () => this.requests.update(list => list.filter(r => r.id !== id)),
    });
  }

  decline(id: string): void {
    this.joinRequestService.rejectRequest(id).subscribe({
      next: () => this.requests.update(list => list.filter(r => r.id !== id)),
    });
  }

  private toDisplay(r: JoinRequestResponse): DisplayRequest {
    return {
      id: r.id,
      initials: r.requestingUsername.substring(0, 2).toUpperCase(),
      username: r.requestingUsername,
      message: r.message ?? '',
      requestDate: new Date(r.createdAt).toLocaleDateString(this.translateService.getCurrentLang() === 'en' ? 'en-US' : 'de-DE'),
    };
  }
}
