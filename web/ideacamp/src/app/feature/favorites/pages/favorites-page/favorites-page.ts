import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProjectFavoriteService } from '../../../../services/project-favorite.service';
import { ProjectResponse } from '../../../../models/project.model';
import { AuthService } from '../../../auth/auth.service';
import { FavoriteButton } from '../../../../shared/favorite-button/favorite-button';

@Component({
  selector: 'app-favorites-page',
  standalone: true,
  imports: [RouterLink, FavoriteButton],
  templateUrl: './favorites-page.html',
})
export class FavoritesPage implements OnInit {
  private readonly projectFavoriteService = inject(ProjectFavoriteService);
  private readonly authService = inject(AuthService);

  readonly projects = signal<ProjectResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');

  async ngOnInit(): Promise<void> {
    await this.authService.waitUntilAuthReady();
    this.loadFavorites();
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
        this.errorMessage.set('Deine Favoriten konnten nicht geladen werden.');
        this.isLoading.set(false);
      },
    });
  }
}
