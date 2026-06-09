import { Component, Input, Output, EventEmitter, OnInit, inject, signal } from '@angular/core';
import { ProjectFavoriteService } from '../../services/project-favorite.service';

@Component({
  selector: 'app-favorite-button',
  standalone: true,
  templateUrl: './favorite-button.html',
})
export class FavoriteButton implements OnInit {
  @Input({ required: true }) projectId!: string;
  @Input() count: number | null = null;
  @Input() onDark = false;
  @Output() favoriteCountChanged = new EventEmitter<number>();

  private readonly favoriteService = inject(ProjectFavoriteService);

  favorited = signal(false);
  loading = signal(true);
  displayCount = signal<number | null>(null);

  ngOnInit(): void {
    this.displayCount.set(this.count);
    this.favoriteService.isFavorited(this.projectId).subscribe(v => {
      this.favorited.set(v);
      this.loading.set(false);
    });
  }

  toggle(event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();

    const previousFavorited = this.favorited();
    const previousCount = this.displayCount();

    this.favorited.set(!previousFavorited);
    if (previousCount !== null) {
      const newCount = previousFavorited ? previousCount - 1 : previousCount + 1;
      this.displayCount.set(newCount);
      this.favoriteCountChanged.emit(newCount);
    }

    const action$ = previousFavorited
      ? this.favoriteService.removeFavorite(this.projectId)
      : this.favoriteService.addFavorite(this.projectId);

    action$.subscribe({
      error: () => {
        this.favorited.set(previousFavorited);
        this.displayCount.set(previousCount);
        if (previousCount !== null) this.favoriteCountChanged.emit(previousCount);
      },
    });
  }
}
