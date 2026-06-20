import { Component, input, output, ChangeDetectionStrategy, inject } from '@angular/core';
import { ProjectInviteResponse } from '../../../../models/project-invite.model';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-invitation-card',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './invitation-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvitationCard {
  readonly invite = input.required<ProjectInviteResponse>();
  readonly accept = output<ProjectInviteResponse>();
  readonly decline = output<ProjectInviteResponse>();

  private readonly translateService = inject(TranslateService);

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

  timeAgo(dateStr: string): string {
    const now = Date.now();
    const date = new Date(dateStr).getTime();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return this.translateService.instant('MYPROJECTS.TIME.JUST_NOW');
    if (diffMins < 60) return this.translateService.instant('MYPROJECTS.TIME.MINUTES_AGO', {count: diffMins});
    if (diffHours < 24) return this.translateService.instant('MYPROJECTS.TIME.HOURS_AGO', {count: diffHours});
    if (diffDays === 1) return this.translateService.instant('MYPROJECTS.TIME.YESTERDAY');
    if (diffDays < 30) return this.translateService.instant('MYPROJECTS.TIME.DAYS_AGO', {count: diffDays});
    return new Date(dateStr).toLocaleDateString();
  }
}
