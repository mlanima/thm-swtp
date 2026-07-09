import { Component, OnInit, inject, signal, DestroyRef, ChangeDetectionStrategy } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../auth/auth.service';
import { MyProjectsService } from '../../../my-projects/services/my-projects.service';
import { MyThesesService } from '../../../my-theses/services/my-theses.service';
import { UserProfileService } from '../../../../services/user-profile.service';
import { ProjectResponse } from '../../../../models/project.model';
import { ThesisResponse } from '../../../../models/thesis.model';
import { InvitationsSection } from '../../../my-projects/components/invitations-section/invitations-section';
import { RecentPosts } from '../../components/recent-posts/recent-posts';
import { CurrentProjects } from '../../components/current-projects/current-projects';
import { CurrentTheses } from '../../components/current-theses/current-theses';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [InvitationsSection, RecentPosts, CurrentProjects, CurrentTheses, TranslatePipe],
  templateUrl: './dashboard-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly myProjectsService = inject(MyProjectsService);
  private readonly myThesesService = inject(MyThesesService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly translateService = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly projects = signal<ProjectResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');
  readonly isProfessor = signal(false);
  readonly theses = signal<ThesisResponse[]>([]);
  readonly isThesesLoading = signal(true);

  ngOnInit(): void {
    this.authService.waitUntilAuthReady().then(() => {
      this.loadProjects();
      this.loadIsProfessor();
    });
  }

  private loadProjects(): void {
    const username = this.authService.username();

    if (!username) {
      this.errorMessage.set(this.translateService.instant('MYPROJECTS.ERROR_LOAD_USERNAME'));
      this.isLoading.set(false);
      return;
    }

    const sub = this.myProjectsService.getRecentProjects(username).subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('MYPROJECTS.ERROR_LOAD_PROJECTS'));
        this.isLoading.set(false);
      },
    });
    this.destroyRef.onDestroy(() => sub.unsubscribe());
  }

  private loadIsProfessor(): void {
    const sub = this.userProfileService.getMyProfile().subscribe({
      next: (profile) => {
        this.isProfessor.set(profile.isProfessor);
        if (profile.isProfessor) {
          this.loadTheses();
        }
      },
    });
    this.destroyRef.onDestroy(() => sub.unsubscribe());
  }

  private loadTheses(): void {
    const username = this.authService.username();
    if (!username) {
      this.isThesesLoading.set(false);
      return;
    }

    const sub = this.myThesesService.getMyTheses(username).subscribe({
      next: (theses) => {
        this.theses.set(theses);
        this.isThesesLoading.set(false);
      },
      error: () => this.isThesesLoading.set(false),
    });
    this.destroyRef.onDestroy(() => sub.unsubscribe());
  }
}
