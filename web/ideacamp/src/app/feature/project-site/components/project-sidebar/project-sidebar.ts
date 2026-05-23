import { Component } from '@angular/core';

import { MemberList } from '../member-list/member-list';
import { TagList } from '../tag-list/tag-list';
import { Quicklinks } from '../quicklinks/quicklinks';
import { TechStack } from '../tech-stack/tech-stack';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [MemberList, TagList, Quicklinks, TechStack],
  templateUrl: './project-sidebar.html'
})
export class ProjectSidebar {}
