import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgClass } from '@angular/common';
import { ProjectService } from '../project-site/project.service';
import { ProjectResponse } from '../../models/project.model';
import { AuthService } from '../auth/auth.service';
import { JoinRequestsTab } from './tabs/join-requests-tab/join-requests-tab';
import { MembersTab } from './tabs/members-tab/members-tab';
import { PrivacyTab } from './tabs/privacy-tab/privacy-tab';
import { DangerZoneTab } from './tabs/danger-zone-tab/danger-zone-tab';

type Tab = 'join-requests' | 'members' | 'privacy' | 'danger-zone';

@Component({
  selector: 'app-project-settings',
  standalone: true,
  imports: [NgClass, RouterLink, JoinRequestsTab, MembersTab, PrivacyTab, DangerZoneTab],
  templateUrl: './project-settings.html',
})
export class ProjectSettings implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly projectService = inject(ProjectService);
  private readonly authService = inject(AuthService);

  project = signal<ProjectResponse | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  activeTab = signal<Tab>('join-requests');

  readonly tabs: { id: Tab; label: string; icon: string }[] = [
    { id: 'join-requests', label: 'Beitrittsanfragen', icon: 'pi-user-plus' },
    { id: 'members',       label: 'Mitglieder',       icon: 'pi-user'      },
    { id: 'privacy',       label: 'Privatsphäre',       icon: 'pi-lock'      },
    { id: 'danger-zone',   label: 'Projekt Löschen',   icon: 'pi-trash'     },
  ];

  ngOnInit(): void {
    const projectUrl = this.route.snapshot.paramMap.get('projectUrl');
    if (!projectUrl) {
      this.errorMessage.set('No project URL provided.');
      this.isLoading.set(false);
      return;
    }

    this.projectService.getProjectByUrl(projectUrl).subscribe({
      next: (data) => {
        const user = this.authService.user();
        if (!user || user.id !== data.ownerId) {
          this.router.navigateByUrl(`/project/${data.projectUrl}`);
          return;
        }
        this.project.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load project.');
        this.isLoading.set(false);
      },
    });
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
  }
}
