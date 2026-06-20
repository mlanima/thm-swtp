import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectJoinRequestService } from '../../services/project-join-request.service';

type JoinButtonStatus = 'loading' | 'idle' | 'pending';

@Component({
  selector: 'app-join-request-button',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './join-request-button.html',
})
export class JoinRequestButton implements OnInit {
  @Input({ required: true }) projectId!: string;
  @Input() onDark = false;
  @Input() allowJoinRequests = true;

  private readonly joinRequestService = inject(ProjectJoinRequestService);

  readonly status = signal<JoinButtonStatus>('loading');

  ngOnInit(): void {
    this.joinRequestService.hasPendingRequest(this.projectId).subscribe(pending => {
      this.status.set(pending ? 'pending' : 'idle');
    });
  }

  send(event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();

    if (this.status() !== 'idle') return;

    this.status.set('pending');

    this.joinRequestService.sendJoinRequest(this.projectId).subscribe({
      error: () => this.status.set('idle'),
    });
  }
}
