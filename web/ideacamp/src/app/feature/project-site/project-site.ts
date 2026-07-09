import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import{CommonModule} from '@angular/common';
import{FormsModule} from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {ProjectService} from './project.service';
import {ProjectResponse } from '../../models/project.model';
import { ProjectHeader } from './components/project-header/project-header';
import { InfoCard } from './components/info-card/info-card';
import { ProjectSidebar } from './components/project-sidebar/project-sidebar';
import {AuthService} from '../auth/auth.service';
import {ToastService} from '../../shared/toast/toast.service';
import { SuccessModal } from '../../shared/success-modal/success-modal';
import { ProjectPosts } from './components/project-posts/project-posts';
import { ReportDialogComponent } from '../reports/components/report-dialog/report-dialog.component';
import { ReportService } from '../reports/service/report-service';
import { ReportReason, ReportTarget } from '../reports/models/report-create.model';
import { ProjectReadme } from './components/project-readme/project-readme';

@Component({
  selector: 'app-project-site',
  standalone: true,
  imports: [
    ProjectHeader,
    InfoCard,
    ProjectSidebar,
    FormsModule,
    CommonModule,
    SuccessModal,
    TranslatePipe,
    ProjectPosts,
    ReportDialogComponent,
    ProjectReadme
  ],
  templateUrl: './project-site.html',
})
export class ProjectSite implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly projectService = inject(ProjectService);
  private readonly authService = inject(AuthService);
  private readonly reportService = inject(ReportService);
  private readonly toastService = inject(ToastService);
  private readonly translateService = inject(TranslateService);

  project = signal<ProjectResponse | null>(null);
  errorMessage = signal<string | null>(null);
  isLoading = signal(true);

  isEditing = signal(false);
  isSaving = signal(false);
  showSuccessModal = signal(false);
  editName = signal('');
  editShortDescription = signal('');
  editDescription = signal('');

  readonly reportDialog = signal<{
    target: ReportTarget;
    targetId: string;
    targetTitle: string;
  } | null>(null);

  readonly isReportSubmitting = signal(false);
  readonly reportErrorMessage = signal<string | null>(null);
  readonly showReportSuccess = signal(false);

  get isOwner(): boolean {
    const user = this.authService.user();
    const proj = this.project();
    if (!user || !proj) return false;
    return user.id === proj.ownerId;
  }

  get canCreatePosts(): boolean {
    return this.isOwner;
  }

  ngOnInit(): void {
    const projectUrl = this.route.snapshot.paramMap.get('projectUrl');
    if (!projectUrl) {
      this.errorMessage.set('PROJECTSITE.ERRORS.NO_PROJECT_URL');
      this.isLoading.set(false);
      return;
    }

    this.projectService.getProjectByUrl(projectUrl).subscribe({
      next: (data) => {
        this.project.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('PROJECTSITE.ERRORS.LOAD_PROJECT');
        this.isLoading.set(false);
      },
    });
  }

  startEdit(): void {
    const proj = this.project();
    if (!proj) return;
    this.editName.set(proj.name);
    this.editShortDescription.set(proj.shortDescription ?? '');
    this.editDescription.set(proj.description ?? '');
    this.isEditing.set(true);
    this.showSuccessModal.set(false);
  }

  onFavoriteCountChanged(newCount: number): void {
    const proj = this.project();
    if (!proj) return;

    this.project.set({
      ...proj,
      favoriteCount: newCount,
      stats: {
        ...proj.stats,
        likes: newCount,
      },
    });
  }

  cancelEdit(): void {
    this.isEditing.set(false);
  }

  saveEdit(): void {
    const proj = this.project();
    if (!proj) return;
    this.isSaving.set(true);

    this.projectService
      .updateProject(proj.id, {
        name: this.editName(),
        shortDescription: this.editShortDescription(),
        description: this.editDescription(),
        projectUrl: proj.projectUrl,
        isPrivateProject: proj.isPrivateProject,
      })
      .subscribe({
        next: (updated) => {
          this.project.set(updated);
          this.isEditing.set(false);
          this.isSaving.set(false);
          this.showSuccessModal.set(true);
        },
        error: (err: HttpErrorResponse) => {
          if (err.error?.errorCode === 'CONTENT_NOT_VALID') {
            this.toastService.error(this.translateService.instant('PROJECTSITE.ERRORS.CONTENT_NOT_VALID'));
          } else {
            this.toastService.error(this.translateService.instant('PROJECTSITE.ERRORS.SAVE_PROJECT'));
          }
          this.isSaving.set(false);
        },
      });
  }

  closeSuccessModal(): void {
    this.showSuccessModal.set(false);
  }

  canReportProject(): boolean {
    const user = this.authService.user();

    return !!user && !this.authService.isModerator() && !this.isOwner;
  }

  openProjectReport(): void {
    const project = this.project();

    if (!project) {
      return;
    }

    this.reportDialog.set({
      target: 'PROJECT',
      targetId: project.id,
      targetTitle: project.name,
    });
    this.reportErrorMessage.set(null);
  }

  openPostReport(post: { id: string; title: string }): void {
    this.reportDialog.set({
      target: 'PROJECT_POST',
      targetId: post.id,
      targetTitle: post.title,
    });
    this.reportErrorMessage.set(null);
  }

  closeReportDialog(): void {
    if (this.isReportSubmitting()) {
      return;
    }

    this.reportDialog.set(null);
    this.reportErrorMessage.set(null);
  }

  submitReport(data: { reason: ReportReason; message?: string }): void {
    const dialog = this.reportDialog();

    if (!dialog) {
      return;
    }

    this.isReportSubmitting.set(true);
    this.reportErrorMessage.set(null);

    this.reportService
      .createReport({
        target: dialog.target,
        targetId: dialog.targetId,
        reason: data.reason,
        message: data.message,
      })
      .subscribe({
        next: () => {
          this.isReportSubmitting.set(false);
          this.closeReportDialog();
          this.showReportSuccess.set(true);
        },
        error: (error) => {
          this.reportErrorMessage.set(
            error.status === 409 ? 'REPORT_DIALOG.ERROR_DUPLICATE' : 'REPORT_DIALOG.ERROR_SEND',
          );
          this.isReportSubmitting.set(false);
        },
      });
  }
  closeReportSuccess(): void {
    this.showReportSuccess.set(false);
  }
}
