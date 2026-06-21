import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { UserFollowService } from '../../services/user-follow.service';

@Component({
  selector: 'app-follow-button',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './follow-button.html',
})
export class FollowButton implements OnInit {
  @Input({ required: true }) username!: string;
  @Input() count = 0;
  @Output() followerCountChanged = new EventEmitter<number>();

  private readonly followService = inject(UserFollowService);

  following = signal(false);
  loading = signal(true);
  displayCount = signal(0);

  ngOnInit(): void {
    this.displayCount.set(this.count);
    this.followService.isFollowing(this.username).subscribe(v => {
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
