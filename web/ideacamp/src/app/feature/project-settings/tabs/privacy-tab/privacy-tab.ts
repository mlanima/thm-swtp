import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../../../project-site/project.service';

@Component({
  selector: 'app-privacy-tab',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './privacy-tab.html',
})
export class PrivacyTab implements OnInit {
  @Input() projectId = '';
  @Input() allowJoinRequests = true;
  @Output() allowJoinRequestsChange = new EventEmitter<boolean>();

  private readonly projectService = inject(ProjectService);

  isPublic = signal(true);
  readonly joinRequestsAllowed = signal(true);

  ngOnInit(): void {
    this.joinRequestsAllowed.set(this.allowJoinRequests);
  }

  toggleAllowJoinRequests(): void {
    const newValue = !this.joinRequestsAllowed();
    this.joinRequestsAllowed.set(newValue);
    this.projectService.updateAllowJoinRequests(this.projectId, newValue).subscribe({
      next: () => this.allowJoinRequestsChange.emit(newValue),
      error: () => this.joinRequestsAllowed.set(!newValue),
    });
  }
}
