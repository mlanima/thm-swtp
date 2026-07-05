import { Component, effect, inject, input, signal, ViewEncapsulation } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { marked } from 'marked';
import { ProjectLinkService } from '../../services/project-link.service';

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

  projectId = input.required<string>();
  refreshToken = input(0);

  repoUrl = signal<string | null>(null);
  renderedHtml = signal<string>('');

  constructor() {
    effect(() => {
      const projectId = this.projectId();
      this.refreshToken();
      this.load(projectId);
    });
  }

  private load(projectId: string): void {
    this.projectLinkService.getProjectReadme(projectId).subscribe({
      next: (readme) => {
        this.repoUrl.set(readme?.repoUrl ?? null);
        this.renderedHtml.set(readme ? (marked.parse(readme.content) as string) : '');
      },
      error: () => {
        this.repoUrl.set(null);
        this.renderedHtml.set('');
      },
    });
  }
}
