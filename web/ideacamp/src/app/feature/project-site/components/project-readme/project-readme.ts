import { Component, effect, inject, input, PLATFORM_ID, signal, ViewEncapsulation } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TranslatePipe } from '@ngx-translate/core';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { ProjectLinkService } from '../../services/project-link.service';

const RELATIVE_URL = /^(?![a-z][a-z0-9+.-]*:|\/\/|#)/i;

@Component({
  selector: 'app-project-readme',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './project-readme.html',
  styleUrl: './project-readme.css',
  encapsulation: ViewEncapsulation.None,
})
export class ProjectReadme {
  private readonly projectLinkService = inject(ProjectLinkService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly platformId = inject(PLATFORM_ID);

  projectId = input.required<string>();
  refreshToken = input(0);

  repoUrl = signal<string | null>(null);
  renderedHtml = signal<SafeHtml>('');

  constructor() {
    effect(() => {
      const projectId = this.projectId();
      this.refreshToken();

      if (isPlatformBrowser(this.platformId)) {
        this.load(projectId);
      }
    });
  }

  private load(projectId: string): void {
    this.projectLinkService.getProjectReadme(projectId).subscribe({
      next: (readme) => {
        this.repoUrl.set(readme?.repoUrl ?? null);

        if (!readme) {
          this.renderedHtml.set('');
          return;
        }

        const rawHtml = marked.parse(readme.content) as string;
        const rewrittenHtml = this.rewriteRelativeUrls(rawHtml, readme.owner, readme.repo);
        const sanitizedHtml = DOMPurify.sanitize(rewrittenHtml);

        this.renderedHtml.set(this.sanitizer.bypassSecurityTrustHtml(sanitizedHtml));
      },
      error: () => {
        this.repoUrl.set(null);
        this.renderedHtml.set('');
      },
    });
  }

  private rewriteRelativeUrls(html: string, owner: string, repo: string): string {
    const rawBase = `https://raw.githubusercontent.com/${owner}/${repo}/HEAD/`;
    const blobBase = `https://github.com/${owner}/${repo}/blob/HEAD/`;

    const template = document.createElement('template');
    template.innerHTML = html;

    template.content.querySelectorAll('img[src]').forEach((img) => {
      const src = img.getAttribute('src') ?? '';
      if (RELATIVE_URL.test(src)) {
        img.setAttribute('src', rawBase + src.replace(/^\.?\//, ''));
      }
    });

    template.content.querySelectorAll('a[href]').forEach((a) => {
      const href = a.getAttribute('href') ?? '';
      if (RELATIVE_URL.test(href)) {
        a.setAttribute('href', blobBase + href.replace(/^\.?\//, ''));
      }
    });

    return template.innerHTML;
  }
}
