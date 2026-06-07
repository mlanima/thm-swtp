import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { ProjectResponse } from '../../../../models/project.model';
import { AuthService } from '../../../auth/auth.service';
import { RouterLink } from '@angular/router';
import { FavoriteButton } from '../../../../shared/favorite-button/favorite-button';

@Component({
  selector: 'app-project-header',
  standalone: true,
  imports: [FavoriteButton, RouterLink],
  templateUrl: './project-header.html'
})
export class ProjectHeader {
  @Input({ required: true }) project!: ProjectResponse;
  @Output() favoriteCountChanged = new EventEmitter<number>();

  private readonly authService = inject(AuthService);

  get isOwner(): boolean {
    const user = this.authService.user();
    if (!user || !this.project) return false;
    return user.id === this.project.ownerId;
  }
}
