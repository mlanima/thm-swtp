import { Component, input, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ThesisResponse } from '../../../../models/thesis.model';
import { getInitials, getAvatarColor } from '../../../../shared/utils/avatar.util';

@Component({
  selector: 'app-current-theses',
  standalone: true,
  imports: [RouterLink, TranslatePipe],
  templateUrl: './current-theses.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .collapsible-content {
      max-height: 0;
      overflow: hidden;
      transition: max-height 0.35s cubic-bezier(0.22, 1, 0.36, 1);
    }
    .collapsible-content.expanded {
      max-height: 5000px;
    }
  `],
})
export class CurrentTheses {
  readonly theses = input.required<ThesisResponse[]>();
  readonly expanded = signal(true);

  toggle(): void {
    this.expanded.update((v) => !v);
  }

  getInitials(name: string): string {
    return getInitials(name);
  }

  getAvatarColor(name: string): string {
    return getAvatarColor(name);
  }
}