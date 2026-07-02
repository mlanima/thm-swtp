import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectView, ManagedProjectSortField, SortDirection } from '../projects.types';

@Component({
  selector: 'app-project-table',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './project-table.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectTable {
  readonly projects = input.required<ProjectView[]>();
  readonly sortField = input.required<ManagedProjectSortField>();
  readonly sortDirection = input.required<SortDirection>();
  readonly sortChange = output<ManagedProjectSortField>();
  readonly deleteProject = output<string>();

  /** Returns the visual sort marker for a sortable project column.*/
  getSortIndicator(field: ManagedProjectSortField): string {
    if (this.sortField() !== field) {
      return '↕';
    }

    return this.sortDirection() === 'asc' ? '↑' : '↓';
  }

  /** Returns the translation key shown when hovering over the current sort state.*/
  getSortTitle(field: ManagedProjectSortField): string {
    if (this.sortField() !== field) {
      return 'MODERATOR.PROJECTS.SORT.SORT_BY';
    }

    return this.sortDirection() === 'asc'
      ? 'MODERATOR.PROJECTS.SORT.ASC'
      : 'MODERATOR.PROJECTS.SORT.DESC';
  }
}
