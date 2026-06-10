import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { ProjectInviteResponse } from '../../../../models/project-invite.model';

@Component({
  selector: 'app-invitation-card',
  standalone: true,
  templateUrl: './invitation-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvitationCard {
  readonly invite = input.required<ProjectInviteResponse>();
  readonly accept = output<ProjectInviteResponse>();
  readonly decline = output<ProjectInviteResponse>();

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

    if (diffMins < 1) return 'gerade eben';
    if (diffMins < 60) return `${diffMins} vor${diffMins === 1 ? '' : 's'} minuten`;
    if (diffHours < 24) return `${diffHours} vor${diffHours === 1 ? '' : 's'} stunden`;
    if (diffDays === 1) return 'gestern';
    if (diffDays < 30) return `${diffDays} vor${diffDays === 1 ? '' : 's'} Tag(en)`;
    return new Date(dateStr).toLocaleDateString();
  }
}
