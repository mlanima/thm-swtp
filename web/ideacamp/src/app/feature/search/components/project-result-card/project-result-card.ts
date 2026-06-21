import { Component, input, signal, inject, DestroyRef, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectSearchResult } from '../../models/project-search-result.model';
import { SearchService } from '../../services/search.service';
import { FavoriteButton } from '../../../../shared/favorite-button/favorite-button';

@Component({
  selector: 'app-project-result-card',
  standalone: true,
  imports: [RouterLink, FavoriteButton, TranslatePipe],
  templateUrl: './project-result-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [
    `
      @keyframes fadeSlideIn {
        from {
          opacity: 0;
          transform: translateY(8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
      .project-card-animate {
        animation: fadeSlideIn 0.3s ease-out both;
      }
    `,
  ],
})
export class ProjectResultCard implements OnInit {
  readonly project = input.required<ProjectSearchResult>();
  readonly tags = signal<string[]>([]);
  readonly animationDelay = input(0);

  private readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const sub = this.searchService.getProjectTags(this.project().id).subscribe({
      next: tags => this.tags.set(tags),
    });
    this.destroyRef.onDestroy(() => sub.unsubscribe());
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .slice(0, 2)
      .map(w => w[0]?.toUpperCase() ?? '')
      .join('');
  }

  getAvatarColor(name: string): string {
    const colors = [
      'bg-purple-500',
      'bg-blue-500',
      'bg-green-500',
      'bg-amber-500',
      'bg-rose-500',
      'bg-cyan-500',
      'bg-indigo-500',
      'bg-teal-500',
    ];
    let hash = 0;
    for (const char of name) {
      hash = char.codePointAt(0)! + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }
}
