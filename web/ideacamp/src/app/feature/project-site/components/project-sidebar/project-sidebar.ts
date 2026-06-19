import { Component, Input } from '@angular/core';

import { TagList } from '../tag-list/tag-list';
import { Quicklinks } from '../quicklinks/quicklinks';
import { MemberList } from '../member-list/member-list';
import { ProjectFiles } from '../project-files/project-files';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [TagList, Quicklinks, MemberList, ProjectFiles],
  templateUrl: './project-sidebar.html'
})
export class ProjectSidebar {
  @Input({ required: true }) projectId!: string;
  @Input() isOwner = false;
  @Input() ownerId = '';
  @Input() ownerUsername = '';
}
