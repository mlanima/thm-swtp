import { Component, OnInit, inject, input, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectGithubRepoService } from '../../services/project-github-repo.service';
import { GithubRepoCardModel } from '../../../../models/github-repo-card.model';

/** Fixed categorical hue order for the language breakdown bar — see the dataviz
 * skill: hues are assigned by position, never cycled or reassigned when the set changes. */
const LANGUAGE_COLORS = ['#2a78d6', '#1baf7a', '#eda100', '#008300', '#4a3aa7', '#e34948'];

@Component({
  selector: 'app-github-repo-card',
  standalone: true,
  imports: [TranslatePipe, DecimalPipe],
  templateUrl: './github-repo-card.html',
})
export class GithubRepoCard implements OnInit {
  private readonly projectGithubRepoService = inject(ProjectGithubRepoService);

  projectId = input.required<string>();

  readonly isLoading = signal(true);
  readonly card = signal<GithubRepoCardModel | null>(null);

  ngOnInit(): void {
    this.projectGithubRepoService.getRepoCard(this.projectId()).subscribe({
      next: (card) => {
        this.card.set(card);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  languageColor(index: number): string {
    return LANGUAGE_COLORS[index % LANGUAGE_COLORS.length];
  }
}
