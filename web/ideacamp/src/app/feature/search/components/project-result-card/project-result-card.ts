import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProjectSearchResult } from '../../models/project-search-result.model';
import { FavoriteButton } from '../../../../shared/favorite-button/favorite-button';

@Component({
  selector: 'app-project-result-card',
  standalone: true,
  imports: [RouterLink, FavoriteButton],
  templateUrl: './project-result-card.html',
})
export class ProjectResultCard {
  @Input({ required: true }) project!: ProjectSearchResult;

  get initials(): string {
    return this.project.name
      .split(' ')
      .slice(0, 2)
      .map(w => w[0]?.toUpperCase() ?? '')
      .join('');
  }
}
