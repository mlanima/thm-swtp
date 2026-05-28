import { Component, Input } from '@angular/core';

import { TagList } from '../tag-list/tag-list';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [TagList,],
  templateUrl: './project-sidebar.html'
})
export class ProjectSidebar {
  @Input({ required: true }) projectId?: string;
  @Input() isOwner = false;
}
