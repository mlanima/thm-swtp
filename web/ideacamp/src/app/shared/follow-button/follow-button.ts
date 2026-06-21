import { Component, EventEmitter, Input, OnInit, OnChanges, OnDestroy, Output, SimpleChanges, inject, signal } from '@angular/core';
import { Subject, merge, takeUntil } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';
import { UserFollowService } from '../../services/user-follow.service';

@Component({
  selector: 'app-follow-button',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './follow-button.html',
})
export class FollowButton implements OnInit, OnChanges, OnDestroy {
  @Input({ required: true }) username!: string;
  @Input() count = 0;
  @Output() followerCountChanged = new EventEmitter<number>();

  private readonly followService = inject(UserFollowService);
  private readonly destroy$ = new Subject<void>();
  private readonly usernameChanged$ = new Subject<void>();

  following = signal(false);
  loading = signal(true);
  displayCount = signal(0);

  ngOnInit(): void {
    this.loadState();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['username'] && !changes['username'].firstChange) {
      this.usernameChanged$.next();
      this.loading.set(true);
      this.following.set(false);
      this.displayCount.set(this.count);
      this.loadState();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadState(): void {
    this.followService.isFollowing(this.username)
      .pipe(takeUntil(merge(this.destroy$, this.usernameChanged$)))
      .subscribe(v => {
        this.following.set(v);
        this.loading.set(false);
      });
  }

  toggle(event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();

    const previousFollowing = this.following();
    const previousCount = this.displayCount();
    const newCount = previousFollowing ? previousCount - 1 : previousCount + 1;

    this.following.set(!previousFollowing);
    this.displayCount.set(newCount);
    this.followerCountChanged.emit(newCount);

    const action$ = previousFollowing
      ? this.followService.unfollowUser(this.username)
      : this.followService.followUser(this.username);

    action$.subscribe({
      error: () => {
        this.following.set(previousFollowing);
        this.displayCount.set(previousCount);
        this.followerCountChanged.emit(previousCount);
      },
    });
  }
}
