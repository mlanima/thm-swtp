import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ThesisResponse } from '../../../../models/thesis.model';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-thesis-card',
  standalone: true,
  imports: [RouterLink, TranslatePipe],
  templateUrl: './thesis-card.html',
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
      .thesis-card-animate {
        animation: fadeSlideIn 0.3s ease-out both;
      }
    `,
  ],
})
export class ThesisCard {
  readonly thesis = input.required<ThesisResponse>();
  readonly animationDelay = input(0);

  getInitials(title: string): string {
    return title
      .split(' ')
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('');
  }

  getAvatarColor(title: string): string {
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
    for (const char of title) {
      hash = char.codePointAt(0)! + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }
}