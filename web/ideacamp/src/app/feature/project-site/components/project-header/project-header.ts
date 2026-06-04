import { Component, Input } from '@angular/core';
import { ProjectResponse } from '../../../../models/project.model';
import { FavoriteButton } from '../../../../shared/favorite-button/favorite-button';

@Component({
  selector: 'app-project-header',
  standalone: true,
  imports: [FavoriteButton],
  templateUrl: './project-header.html'
})
export class ProjectHeader {
  @Input({ required: true }) project!: ProjectResponse;
  @Input() isOwner = false;
}
