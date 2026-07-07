import { Component, OnInit, PLATFORM_ID, ViewEncapsulation, inject, input, signal } from '@angular/core';
import { Location, isPlatformBrowser } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
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
  private readonly sanitizer = inject(DomSanitizer);
  private readonly location = inject(Location);

  projectId = input.required<string>();

  readonly html = signal<SafeHtml | null>(null);

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.projectGithubRepoService.getReadme(this.projectId()).subscribe({
      next: (readme) => {
        if (!readme?.available || !readme.markdown) {
          return;
        }
        const rendered = renderGithubReadme(
          readme.markdown, readme.repoOwner, readme.repoName, readme.defaultBranch, this.location.path(),
        );
        // DOMPurify (in renderGithubReadme) is the actual trust boundary for this
        // repo-owner-controlled content. Angular's own [innerHTML] sanitizer is more
        // restrictive than necessary on top of that and was observed stripping legitimate
        // markup (e.g. `<a name="...">` anchors, common for README "back to top" links).
        this.html.set(this.sanitizer.bypassSecurityTrustHtml(rendered));
      },
      error: () => {
        // README section is an opt-in nicety — stay hidden rather than showing an error.
      },
    });
  }
}
