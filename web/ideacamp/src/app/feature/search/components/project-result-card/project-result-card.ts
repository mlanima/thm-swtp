import { Component, input, signal, inject, DestroyRef, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProjectSearchResult } from '../../models/project-search-result.model';
import { SearchService } from '../../services/search.service';
import { FavoriteButton } from '../../../../shared/favorite-button/favorite-button';

@Component({
  selector: 'app-project-result-card',
  standalone: true,
  imports: [RouterLink, FavoriteButton],
  templateUrl: './project-result-card.html',
})
export class ProjectResultCard implements OnInit {
  readonly project = input.required<ProjectSearchResult>();
  readonly tags = signal<string[]>([]);

  private readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const sub = this.searchService.getProjectTags(this.project().id).subscribe({
      next: tags => this.tags.set(tags),
    });
    this.destroyRef.onDestroy(() => sub.unsubscribe());
  }

  get initials(): string {
    return this.project().name
      .split(' ')
      .slice(0, 2)
      .map(w => w[0]?.toUpperCase() ?? '')
      .join('');
  }
}
