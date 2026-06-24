import { Component, OnInit, signal, computed, inject, DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ModeratorProjectsService, ProjectSearchParams } from '../services/moderator-projects.service';
import { ProjectResponse } from '../../../models/project.model';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

interface ProjectView {
  id: string;
  name: string;
  memberCount: number;
  ownerUsername: string;
  ownerInitials: string;
  createdAt: string;
  isPrivate: boolean;
}

interface DeleteState {
  projectId: string;
  projectName: string;
}

const PAGE_SIZE = 10;

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './projects.component.html',
  styleUrl: './projects.component.css',
})
export class ProjectsComponent implements OnInit {
  private readonly moderatorProjectsService = inject(ModeratorProjectsService);
  private readonly translateService = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly searchQuery = signal('');
  readonly projects = signal<ProjectView[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly deleteState = signal<DeleteState | null>(null);
  readonly deleteConfirmInput = signal('');
  readonly isDeleting = signal(false);
  readonly deleteError = signal('');

  readonly isDeleteEnabled = computed(() => {
    const state = this.deleteState();
    return state !== null && this.deleteConfirmInput() === state.projectName;
  });

  private readonly searchSubject = new Subject<string>();

  ngOnInit(): void {
    const sub = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((query) => {
        this.currentPage.set(0);
        this.loadProjects(0, query);
      });
    this.destroyRef.onDestroy(() => sub.unsubscribe());

    this.loadProjects(0, '');
  }

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
    this.searchSubject.next(value);
  }

  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.currentPage()) return;
    this.currentPage.set(page);
    this.loadProjects(page, this.searchQuery());
  }

  getPageNumbers(): number[] {
    const total = this.totalPages();
    const current = this.currentPage();
    const pages: number[] = [];
    const start = Math.max(0, current - 2);
    const end = Math.min(total - 1, current + 2);
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }

  openDeleteDialog(projectId: string, projectName: string): void {
    this.deleteState.set({ projectId, projectName });
    this.deleteConfirmInput.set('');
    this.deleteError.set('');
  }

  closeDeleteDialog(): void {
    this.deleteState.set(null);
    this.deleteConfirmInput.set('');
    this.isDeleting.set(false);
    this.deleteError.set('');
  }

  confirmDelete(): void {
    const state = this.deleteState();
    if (!state || this.isDeleting() || !this.isDeleteEnabled()) return;

    this.isDeleting.set(true);
    this.deleteError.set('');

    this.moderatorProjectsService.deleteProject(state.projectId).subscribe({
      next: () => {
        this.projects.update((list) => list.filter((p) => p.id !== state.projectId));
        this.closeDeleteDialog();
      },
      error: () => {
        this.deleteError.set(
          this.translateService.instant('MODERATOR.PROJECTS.ERROR_DELETE'),
        );
        this.isDeleting.set(false);
      },
    });
  }

  private loadProjects(page: number, name: string): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    const params: ProjectSearchParams = { page, size: PAGE_SIZE };
    if (name.trim()) {
      params.name = name.trim();
    }

    this.moderatorProjectsService.getAllProjects(params).subscribe({
      next: (pageResponse) => {
        this.projects.set(pageResponse.content.map((p) => this.toProjectView(p)));
        this.currentPage.set(pageResponse.number);
        this.totalPages.set(pageResponse.totalPages);
        this.totalElements.set(pageResponse.totalElements);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(
          this.translateService.instant('MODERATOR.PROJECTS.ERROR_LOAD'),
        );
        this.isLoading.set(false);
      },
    });
  }

  private toProjectView(p: ProjectResponse): ProjectView {
    return {
      id: p.id,
      name: p.name,
      memberCount: p.memberIds.length,
      ownerUsername: p.ownerUsername,
      ownerInitials: this.getInitials(p.ownerUsername),
      createdAt: this.formatDate(p.createdAt),
      isPrivate: p.isPrivateProject,
    };
  }

  private getInitials(username: string): string {
    return username.charAt(0).toUpperCase();
  }

  private formatDate(iso: string): string {
    const date = new Date(iso);
    return date.toLocaleDateString('de-DE', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }
}
