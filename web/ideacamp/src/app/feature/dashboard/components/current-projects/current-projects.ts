import { Component, input, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectResponse } from '../../../../models/project.model';
import { FavoriteButton } from '../../../../shared/favorite-button/favorite-button';
import { getInitials, getAvatarColor } from '../../../../shared/utils/avatar.util';

@Component({
  selector: 'app-current-projects',
  standalone: true,
  imports: [RouterLink, TranslatePipe, FavoriteButton],
  templateUrl: './current-projects.html',
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
export class CurrentProjects {
  readonly projects = input.required<ProjectResponse[]>();
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
