import { Component, Input } from '@angular/core';

import { TagList } from '../tag-list/tag-list';
import { Quicklinks } from '../quicklinks/quicklinks'

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [TagList, Quicklinks],
  templateUrl: './project-sidebar.html'
})
export class ProjectSidebar {
  @Input({ required: true }) projectId!: string;
  @Input() isOwner = false;
}
