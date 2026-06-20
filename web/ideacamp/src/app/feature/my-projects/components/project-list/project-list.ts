import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { ProjectResponse } from '../../../../models/project.model';
import { ProjectCard } from '../project-card/project-card';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [ProjectCard, TranslatePipe],
  templateUrl: './project-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectList {
  readonly projects = input.required<ProjectResponse[]>();
  readonly projectTags = input.required<Map<string, string[]>>();
  readonly isLoading = input.required<boolean>();
  readonly isFiltering = input.required<boolean>();
  readonly errorMessage = input.required<string>();

  getTagsForProject(projectId: string): string[] {
    return this.projectTags().get(projectId) ?? [];
  }
}
