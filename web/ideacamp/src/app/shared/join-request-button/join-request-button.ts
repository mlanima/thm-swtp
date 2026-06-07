import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { ProjectJoinRequestService } from '../../services/project-join-request.service';

@Component({
  selector: 'app-join-request-button',
  standalone: true,
  templateUrl: './join-request-button.html',
})
export class JoinRequestButton implements OnInit {
  @Input({ required: true }) projectId!: string;
  @Input() onDark = false;
  @Input() allowJoinRequests = true;

  private readonly joinRequestService = inject(ProjectJoinRequestService);

  readonly loading = signal(true);
  readonly isPending = signal(false);

  ngOnInit(): void {
    this.joinRequestService.hasPendingRequest(this.projectId).subscribe(pending => {
      this.isPending.set(pending);
      this.loading.set(false);
    });
  }

  send(event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();

    if (this.isPending()) return;

    this.isPending.set(true);

    this.joinRequestService.sendJoinRequest(this.projectId).subscribe({
      error: () => this.isPending.set(false),
    });
  }
}
