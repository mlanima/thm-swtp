import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProjectSettingsStore } from '../../project-settings.store';
import { ProjectGithubRepoService } from '../../../project-site/services/project-github-repo.service';
import { GithubRepoCardModel } from '../../../../models/github-repo-card.model';

function parseRepoInput(value: string): { repoOwner: string; repoName: string } | null {
  const trimmed = value.trim();

  const urlMatch = /github\.com\/([^/\s]+)\/([^/\s]+?)(?:\.git)?\/?$/i.exec(trimmed);
  if (urlMatch) {
    return { repoOwner: urlMatch[1], repoName: urlMatch[2] };
  }

  const parts = trimmed.split('/');
  if (parts.length === 2 && parts[0] && parts[1]) {
    return { repoOwner: parts[0], repoName: parts[1] };
  }

  return null;
}

@Component({
  selector: 'app-github-tab',
  standalone: true,
  imports: [FormsModule, RouterLink, TranslatePipe],
  templateUrl: './github-tab.html',
})
export class GithubTab implements OnInit {
  private readonly store = inject(ProjectSettingsStore);
  private readonly projectGithubRepoService = inject(ProjectGithubRepoService);
  private readonly translate = inject(TranslateService);

  readonly isLoading = signal(true);
  readonly repoCard = signal<GithubRepoCardModel | null>(null);
  readonly repoInput = signal('');
  readonly isLinking = signal(false);
  readonly isUnlinking = signal(false);
  readonly isTogglingReadme = signal(false);
  readonly isTogglingAutoInvite = signal(false);
  readonly errorMessage = signal('');
  readonly needsGithubConnection = signal(false);

  ngOnInit(): void {
    const project = this.store.project();
    if (!project) {
      this.isLoading.set(false);
      return;
    }

    this.projectGithubRepoService.getRepoCard(project.id).subscribe({
      next: (card) => {
        this.repoCard.set(card);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('GITHUB.REPO.ERROR_LOAD'));
        this.isLoading.set(false);
      },
    });
  }

  linkRepo(): void {
    const project = this.store.project();
    if (!project) return;

    const parsed = parseRepoInput(this.repoInput());
    if (!parsed) {
      this.errorMessage.set(this.translate.instant('GITHUB.REPO.INVALID_INPUT'));
      return;
    }

    this.errorMessage.set('');
    this.needsGithubConnection.set(false);
    this.isLinking.set(true);

    this.projectGithubRepoService.linkRepo(project.id, parsed).subscribe({
      next: (card) => {
        this.repoCard.set(card);
        this.repoInput.set('');
        this.isLinking.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 409) {
          this.needsGithubConnection.set(true);
          this.errorMessage.set(this.translate.instant('GITHUB.REPO.CONNECTION_REQUIRED'));
        } else if (error.status === 403) {
          this.errorMessage.set(this.translate.instant('GITHUB.REPO.ACCESS_DENIED'));
        } else if (error.status === 400) {
          this.errorMessage.set(this.translate.instant('GITHUB.REPO.INVALID_INPUT'));
        } else if (error.status === 404) {
          this.errorMessage.set(this.translate.instant('GITHUB.REPO.NOT_FOUND'));
        } else {
          this.errorMessage.set(this.translate.instant('GITHUB.REPO.ERROR_LINK'));
        }
        this.isLinking.set(false);
      },
    });
  }

  toggleShowReadme(): void {
    const project = this.store.project();
    const currentCard = this.repoCard();
    if (!project || !currentCard || this.isTogglingReadme()) return;

    this.isTogglingReadme.set(true);
    this.errorMessage.set('');

    this.projectGithubRepoService.setReadmeVisibility(project.id, !currentCard.showReadme).subscribe({
      next: (card) => {
        this.repoCard.set(card);
        this.isTogglingReadme.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('GITHUB.REPO.ERROR_README_TOGGLE'));
        this.isTogglingReadme.set(false);
      },
    });
  }

  toggleAutoInviteCollaborators(): void {
    const project = this.store.project();
    const currentCard = this.repoCard();
    if (!project || !currentCard || this.isTogglingAutoInvite()) return;

    this.isTogglingAutoInvite.set(true);
    this.errorMessage.set('');

    this.projectGithubRepoService
      .setAutoInviteCollaborators(project.id, !currentCard.autoInviteCollaborators)
      .subscribe({
        next: (card) => {
          this.repoCard.set(card);
          this.isTogglingAutoInvite.set(false);
        },
        error: () => {
          this.errorMessage.set(this.translate.instant('GITHUB.REPO.ERROR_AUTO_INVITE_TOGGLE'));
          this.isTogglingAutoInvite.set(false);
        },
      });
  }

  unlinkRepo(): void {
    const project = this.store.project();
    if (!project) return;

    this.isUnlinking.set(true);
    this.errorMessage.set('');

    this.projectGithubRepoService.unlinkRepo(project.id).subscribe({
      next: () => {
        this.repoCard.set(null);
        this.isUnlinking.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('GITHUB.REPO.ERROR_UNLINK'));
        this.isUnlinking.set(false);
      },
    });
  }
}
