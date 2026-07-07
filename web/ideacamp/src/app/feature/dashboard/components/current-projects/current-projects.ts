import { Component, input, ChangeDetectionStrategy } from '@angular/core';
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
})
export class CurrentProjects {
  readonly projects = input.required<ProjectResponse[]>();

  getInitials(name: string): string {
    return getInitials(name);
  }

  getAvatarColor(name: string): string {
    return getAvatarColor(name);
  }
}
