import { Component, OnInit, PLATFORM_ID, ViewEncapsulation, inject, input, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ProjectGithubRepoService } from '../../services/project-github-repo.service';
import { renderGithubReadme } from '../../../../shared/utils/github-readme-renderer';

@Component({
  selector: 'app-project-readme',
  standalone: true,
  templateUrl: './project-readme.html',
  styleUrl: './project-readme.css',
  // The README body is raw HTML from marked/DOMPurify, injected via [innerHTML] — it never
  // passes through Angular's template compiler, so component-scoped (emulated) styles can't
  // reach it. Encapsulation is turned off for this component only; every rule below is scoped
  // under .readme-content to avoid leaking globally.
  encapsulation: ViewEncapsulation.None,
})
export class ProjectReadme implements OnInit {
  private readonly projectGithubRepoService = inject(ProjectGithubRepoService);
  private readonly platformId = inject(PLATFORM_ID);

  projectId = input.required<string>();

  readonly html = signal<string | null>(null);

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.projectGithubRepoService.getReadme(this.projectId()).subscribe({
      next: (readme) => {
        if (!readme?.available || !readme.markdown) {
          return;
        }
        this.html.set(
          renderGithubReadme(readme.markdown, readme.repoOwner, readme.repoName, readme.defaultBranch),
        );
      },
      error: () => {
        // README section is an opt-in nicety — stay hidden rather than showing an error.
      },
    });
  }
}
