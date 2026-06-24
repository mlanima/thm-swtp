import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectView } from '../projects.types';

@Component({
  selector: 'app-project-table',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './project-table.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectTable {
  readonly projects = input.required<ProjectView[]>();
  readonly deleteProject = output<string>();
}
