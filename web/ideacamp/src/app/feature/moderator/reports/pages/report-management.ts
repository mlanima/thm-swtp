import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { Pagination } from '../../shared/pagination/pagination';
import { ModeratorReportManagementService } from '../service/moderator-report-management.service';
import {
  ManagedReport,
  ReportReason,
  ReportSortField,
  ReportStatus,
  ReportTarget,
  SortDirection,
  TargetAction,
} from '../models/report.model';
import { ReportTableComponent } from '../components/report-table/report-table.component';
import { ReportDetailDialogComponent } from '../components/report-detail-dialog/report-detail-dialog.component';
import { ReportTargetActionService } from '../service/report-target-action.service';
import { BanUserDialogComponent, BanUserDialogUser } from '../../user-management/components/ban-user-dialog/ban-user-dialog.component';
import { DeleteDialog } from '../../projects/delete-dialog/delete-dialog';
import { DeleteState } from '../../projects/projects.types';
import { finalize } from 'rxjs';


const PAGE_SIZE = 20;

@Component({
  selector: 'app-report-management',
  standalone: true,
  imports: [
    FormsModule,
    TranslatePipe,
    Pagination,
    ReportTableComponent,
    ReportDetailDialogComponent,
    BanUserDialogComponent,
    DeleteDialog,
  ],
  templateUrl: './report-management.html',
})
export class ReportManagement implements OnInit {
  private readonly reportService = inject(ModeratorReportManagementService);
  private readonly targetActionService = inject(ReportTargetActionService);

  reports = signal<ManagedReport[]>([]);
  selectedReport = signal<ManagedReport | null>(null);

  query = signal('');
  statusFilter = signal<ReportStatus | null>(null);
  targetFilter = signal<ReportTarget | null>(null);
  reasonFilter = signal<ReportReason | null>(null);

  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);

  sortField = signal<ReportSortField>('createdAt');
  sortDirection = signal<SortDirection>('desc');

  isLoading = signal(false);
  isUpdating = signal(false);
  errorMessage = signal<string | null>(null);

  userToBan = signal<BanUserDialogUser | null>(null);
  projectToDelete = signal<DeleteState | null>(null);
  postToDelete = signal<ManagedReport | null>(null);

  reportForTargetAction = signal<ManagedReport | null>(null);

  deleteConfirmInput = signal('');
  isTargetActionRunning = signal(false);
  targetActionError = signal<string | null>(null);

  private queryDebounceTimeout: ReturnType<typeof setTimeout> | null = null;

  readonly statusOptions: (ReportStatus | null)[] = [
    null,
    'OPEN',
    'IN_REVIEW',
    'RESOLVED',
    'DISMISSED',
  ];

  readonly targetOptions: (ReportTarget | null)[] = [null, 'USER', 'PROJECT', 'PROJECT_POST'];

  readonly reasonOptions: ReportReason[] = [
    'SELF_HARM_OR_SUICIDE',
    'VIOLENCE_OR_THREATS',
    'HATE_SPEECH',
    'HARASSMENT',
    'PERSONAL_DATA',
    'FRAUD_OR_IMPERSONATION',
    'SPAM',
    'COPYRIGHT',
    'MISINFORMATION',
    'INAPPROPRIATE_CONTENT',
    'OTHER',
  ];

  ngOnInit(): void {
    this.loadReports(0);
  }

  loadReports(page: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.reportService
      .getReports({
        page,
        size: PAGE_SIZE,
        status: this.statusFilter() ?? undefined,
        target: this.targetFilter() ?? undefined,
        reason: this.reasonFilter() ?? undefined,
        query: this.query(),
        sortField: this.sortField(),
        sortDirection: this.sortDirection(),
      })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          this.reports.set(response.content);
          this.currentPage.set(response.page);
          this.totalPages.set(response.totalPages);
          this.totalElements.set(response.totalElements);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set('MODERATOR.REPORTS.ERROR_LOAD');
          this.isLoading.set(false);
        },
      });
  }

  onQueryChange(value: string): void {
    this.query.set(value);

    if (this.queryDebounceTimeout) {
      clearTimeout(this.queryDebounceTimeout);
    }

    this.queryDebounceTimeout = setTimeout(() => {
      this.loadReports(0);
    }, 300);
  }

  setStatusFilter(status: ReportStatus | null): void {
    this.statusFilter.set(status);
    this.loadReports(0);
  }

  setTargetFilter(target: ReportTarget | null): void {
    this.targetFilter.set(target);
    this.loadReports(0);
  }

  setReasonFilter(reason: ReportReason | null): void {
    this.reasonFilter.set(reason);
    this.loadReports(0);
  }

  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.currentPage()) {
      return;
    }

    this.loadReports(page);
  }

  changeSort(field: ReportSortField): void {
    if (this.sortField() === field) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDirection.set('asc');
    }

    this.loadReports(0);
  }

  openReport(report: ManagedReport): void {
    this.selectedReport.set(report);
  }

  closeReportDialog(): void {
    this.selectedReport.set(null);
  }

  updateReportStatusFromTable(event: { report: ManagedReport; status: ReportStatus }): void {
    this.updateStatusOfReport(event.report, event.status);
  }

  updateReportStatus(status: ReportStatus, moderatorMessage?: string): void {
    const report = this.selectedReport();

    if (!report) {
      return;
    }

    this.updateStatusOfReport(report, status, moderatorMessage);
  }

  /** Opens the matching confirmation dialog for the selected report target action.*/
  handleTargetAction(event: { report: ManagedReport; action: TargetAction }): void {
    this.selectedReport.set(null);
    this.reportForTargetAction.set(event.report);
    this.targetActionError.set(null);

    switch (event.action) {
      case 'BAN_USER':
        this.userToBan.set({
          keycloakId: event.report.targetId,
          username: event.report.targetSummary?.title ?? event.report.targetId,
          email: null,
        });
        return;

      case 'DELETE_PROJECT':
        this.projectToDelete.set({
          projectId: event.report.targetId,
          projectName: event.report.targetSummary?.title ?? event.report.targetId,
        });
        this.deleteConfirmInput.set('');
        return;

      case 'DELETE_PROJECT_POST':
        this.postToDelete.set(event.report);
        return;
    }
  }

  confirmBanUserFromReport(reason?: string): void {
    const report = this.reportForTargetAction();

    if (!report) {
      return;
    }

    this.isTargetActionRunning.set(true);
    this.targetActionError.set(null);

    this.targetActionService.banUser(report.targetId, reason).subscribe({
      next: () => {
        this.userToBan.set(null);
        this.markReportResolvedAfterTargetAction('User was banned after report review.');
      },
      error: () => {
        this.targetActionError.set('MODERATOR.REPORTS.ERROR_TARGET_ACTION');
        this.isTargetActionRunning.set(false);
      },
    });
  }

  closeBanUserDialog(): void {
    if (this.isTargetActionRunning()) {
      return;
    }

    this.userToBan.set(null);
    this.reportForTargetAction.set(null);
  }

  isProjectDeleteEnabled(): boolean {
    const state = this.projectToDelete();

    if (!state) {
      return false;
    }

    return this.deleteConfirmInput().trim() === state.projectName;
  }

  confirmDeleteProjectFromReport(): void {
    const state = this.projectToDelete();

    if (!state) {
      return;
    }

    this.isTargetActionRunning.set(true);
    this.targetActionError.set(null);

    this.targetActionService.deleteProject(state.projectId).subscribe({
      next: () => {
        this.projectToDelete.set(null);
        this.deleteConfirmInput.set('');
        this.markReportResolvedAfterTargetAction('Project was deleted after report review.');
      },
      error: () => {
        this.targetActionError.set('MODERATOR.REPORTS.ERROR_TARGET_ACTION');
        this.isTargetActionRunning.set(false);
      },
    });
  }

  closeProjectDeleteDialog(): void {
    if (this.isTargetActionRunning()) {
      return;
    }

    this.projectToDelete.set(null);
    this.deleteConfirmInput.set('');
    this.reportForTargetAction.set(null);
  }

  /** Deletes the reported project post using the parent project id from the target summary.*/
  confirmDeleteProjectPostFromReport(): void {
    const report = this.postToDelete();
    const projectId = report?.targetSummary?.parentId;

    if (!report || !projectId) {
      this.targetActionError.set('MODERATOR.REPORTS.ERROR_TARGET_ACTION');
      return;
    }

    this.isTargetActionRunning.set(true);
    this.targetActionError.set(null);

    this.targetActionService.deleteProjectPost(projectId, report.targetId).subscribe({
      next: () => {
        this.postToDelete.set(null);
        this.markReportResolvedAfterTargetAction('Project post was deleted after report review.');
      },
      error: () => {
        this.targetActionError.set('MODERATOR.REPORTS.ERROR_TARGET_ACTION');
        this.isTargetActionRunning.set(false);
      },
    });
  }

  closeProjectPostDeleteDialog(): void {
    if (this.isTargetActionRunning()) {
      return;
    }

    this.postToDelete.set(null);
    this.reportForTargetAction.set(null);
  }

  /** Resolves all active reports for the moderated target after the target action succeeded.*/
  private markReportResolvedAfterTargetAction(moderatorMessage: string): void {
    const report = this.reportForTargetAction();

    if (!report) {
      this.isTargetActionRunning.set(false);
      return;
    }

    this.reportService
      .resolveActiveReportsForTarget(report.target, report.targetId, moderatorMessage)
      .subscribe({
        next: () => {
          this.selectedReport.set(null);
          this.reportForTargetAction.set(null);
          this.isTargetActionRunning.set(false);
          this.loadReports(this.currentPage());
        },
        error: () => {
          this.targetActionError.set('MODERATOR.REPORTS.ERROR_UPDATE');
          this.isTargetActionRunning.set(false);
        },
      });
  }

  private updateStatusOfReport(
    report: ManagedReport,
    status: ReportStatus,
    moderatorMessage?: string,
  ): void {
    this.isUpdating.set(true);
    this.errorMessage.set(null);

    this.reportService.updateReportStatus(report.id, { status, moderatorMessage }).subscribe({
      next: (updatedReport) => {
        this.reports.update((reports) =>
          reports.map((currentReport) =>
            currentReport.id === updatedReport.id ? updatedReport : currentReport,
          ),
        );

        if (this.selectedReport()?.id === updatedReport.id) {
          this.selectedReport.set(updatedReport);
        }
        this.isUpdating.set(false);
      },
      error: () => {
        this.errorMessage.set('MODERATOR.REPORTS.ERROR_UPDATE');
        this.isUpdating.set(false);
      },
    });
  }
}
