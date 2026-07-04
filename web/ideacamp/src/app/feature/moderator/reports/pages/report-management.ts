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
} from '../models/report.model';
import { ReportTableComponent } from '../components/report-table/report-table.component';
import { ReportDetailDialogComponent } from '../components/report-detail-dialog/report-detail-dialog.component';


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
  ],
  templateUrl: './report-management.html',
})
export class ReportManagement implements OnInit {
  private readonly reportService = inject(ModeratorReportManagementService);

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
      .subscribe({
        next: (response) => {
          this.reports.set(response.content);
          this.currentPage.set(response.number);
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
    this.loadReports(0);
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

  updateReportStatus(status: ReportStatus, moderatorMessage?: string): void {
    const report = this.selectedReport();

    if (!report) {
      return;
    }

    this.isUpdating.set(true);

    this.reportService
      .updateReportStatus(report.id, {
        status,
        moderatorMessage,
      })
      .subscribe({
        next: (updatedReport) => {
          this.selectedReport.set(updatedReport);
          this.reports.update((reports) =>
            reports.map((currentReport) =>
              currentReport.id === updatedReport.id ? updatedReport : currentReport,
            ),
          );
          this.isUpdating.set(false);
        },
        error: () => {
          this.errorMessage.set('MODERATOR.REPORTS.ERROR_UPDATE');
          this.isUpdating.set(false);
        },
      });
  }
  updateReportStatusFromTable(event: { report: ManagedReport; status: ReportStatus }): void {
    this.selectedReport.set(event.report);
    this.updateReportStatus(event.status);
  }
}
