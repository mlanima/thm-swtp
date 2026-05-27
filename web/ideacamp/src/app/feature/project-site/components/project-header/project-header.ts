import { Component, Input } from '@angular/core';
import { ProjectResponse } from '../../../../models/project.model';

@Component({
  selector: 'app-project-header',
  standalone: true,
  imports: [],
  templateUrl: './project-header.html'
})
export class ProjectHeader {
  @Input({ required: true }) project!: ProjectResponse;
  @Input() isOwner = false;
}
