import { Component } from '@angular/core';

import { ProjectHeader } from '../../components/project-header/project-header';
import { InfoCard } from '../../components/info-card/info-card';
import { OpenPositionCard } from '../../components/open-position-card/open-position-card';
import { ProjectSidebar } from '../../components/project-sidebar/project-sidebar';

@Component({
  selector: 'app-project-site',
  standalone: true,
  imports: [ProjectHeader, InfoCard, OpenPositionCard, ProjectSidebar],
  templateUrl: './project-site.html',
})
export class ProjectSite {}
