import { Component, Input, inject } from '@angular/core';
import { ProjectResponse } from '../../../../models/project.model';
import {AuthService} from '../../../auth/auth.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-project-header',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './project-header.html'
})
export class ProjectHeader {
  @Input({ required: true }) project!: ProjectResponse;
  //@Input() isOwner = false;

  private readonly authService = inject(AuthService);

  get isOwner(): boolean {
    const user = this.authService.user();
    if (!user || !this.project) {
      return false;
    }
    return user.id === this.project.ownerId;
  }
}
