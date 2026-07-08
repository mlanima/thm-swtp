import { Component, inject, input, computed } from '@angular/core';
import { TagList } from '../tag-list/tag-list';
import { MemberList } from '../member-list/member-list';
import { LinkManagerComponent } from '../../../../shared/link-manager/link-manager';
import { LinkManagerDataSource } from '../../../../shared/link-manager/link-manager.types';
import { ProjectLinkModel } from '../../../../models/project-link.model';
import { ProjectLinkService } from '../../services/project-link.service';
import { ProjectFiles } from '../project-files/project-files';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [TagList, LinkManagerComponent, MemberList, ProjectFiles],
  templateUrl: './project-sidebar.html',
})
export class ProjectSidebar {
  private readonly projectLinkService = inject(ProjectLinkService);

  projectId = input.required<string>();
  isOwner = input(false);
  ownerId = input('');
  ownerUsername = input('');

  projectLinkDataSource = computed<LinkManagerDataSource<ProjectLinkModel>>(() => {
    const projectId = this.projectId();

    return {
      load: () => this.projectLinkService.getProjectLinks(projectId),
      createLink: (request) =>
        this.projectLinkService.addProjectLink(projectId, {
          label: request.label,
          url: request.url,
          visibility: request.visibility ?? 'PUBLIC',
        }),

      updateLink: (linkId, request) =>
        this.projectLinkService.updateProjectLink(projectId, linkId, {
          label: request.label,
          url: request.url,
          visibility: request.visibility,
        }),
      deleteLink: (linkId) => this.projectLinkService.deleteProjectLink(projectId, linkId),
    };
  });
}
