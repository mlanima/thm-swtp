import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProjectFavoriteService } from '../../../../services/project-favorite.service';
import { ProjectResponse } from '../../../../models/project.model';
import { AuthService } from '../../../auth/auth.service';
import { FavoriteButton } from '../../../../shared/favorite-button/favorite-button';

@Component({
  selector: 'app-favorites-page',
  standalone: true,
  imports: [RouterLink, FavoriteButton, TranslatePipe],
  templateUrl: './favorites-page.html',
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
      .favorite-card-animate {
        animation: fadeSlideIn 0.3s ease-out both;
      }
    `,
  ],
})
export class FavoritesPage implements OnInit {
  private readonly projectFavoriteService = inject(ProjectFavoriteService);
  private readonly authService = inject(AuthService);
  private readonly translateService = inject(TranslateService);

  readonly projects = signal<ProjectResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');

  async ngOnInit(): Promise<void> {
    await this.authService.waitUntilAuthReady();
    this.loadFavorites();
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? '')
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

  private loadFavorites(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.projectFavoriteService.getFavorites().subscribe({
      next: projects => {
        this.projects.set(projects);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('FAVORITE.ERROR_LOAD'));
        this.isLoading.set(false);
      },
    });
  }
}
