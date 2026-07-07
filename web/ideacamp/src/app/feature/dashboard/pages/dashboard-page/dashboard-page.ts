import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../auth/auth.service';
import { MyProjectsService } from '../../../my-projects/services/my-projects.service';
import { ProjectResponse } from '../../../../models/project.model';
import { InvitationsSection } from '../../../my-projects/components/invitations-section/invitations-section';
import { RecentPosts } from '../../components/recent-posts/recent-posts';
import { CurrentProjects } from '../../components/current-projects/current-projects';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [InvitationsSection, RecentPosts, CurrentProjects, TranslatePipe],
  templateUrl: './dashboard-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly myProjectsService = inject(MyProjectsService);
  private readonly translateService = inject(TranslateService);

  readonly projects = signal<ProjectResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');

  ngOnInit(): void {
    this.authService.waitUntilAuthReady().then(() => this.loadProjects());
  }

  private loadProjects(): void {
    const username = this.authService.username();

    if (!username) {
      this.errorMessage.set(this.translateService.instant('MYPROJECTS.ERROR_LOAD_USERNAME'));
      this.isLoading.set(false);
      return;
    }

    this.myProjectsService.getRecentProjects(username).subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('MYPROJECTS.ERROR_LOAD_PROJECTS'));
        this.isLoading.set(false);
      },
    });
  }
}
