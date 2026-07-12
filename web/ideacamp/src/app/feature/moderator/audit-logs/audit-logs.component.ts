import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuditLog } from './models/audit-log.model';
import { AuditLogsService } from '../services/audit-logs.service';
import { Pagination } from '../shared/pagination/pagination';
import { createPageRange } from '../shared/pagination/page-range';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, TranslatePipe, Pagination],
  templateUrl: './audit-logs.component.html',
})
export class AuditLogsComponent implements OnInit {
  private readonly auditLogsService = inject(AuditLogsService);
  private readonly translateService = inject(TranslateService);

  readonly auditLogs = signal<AuditLog[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  private readonly pageRange = createPageRange(this.auditLogs, this.currentPage, PAGE_SIZE);
  readonly rangeStart = this.pageRange.start;
  readonly rangeEnd = this.pageRange.end;

  ngOnInit(): void {
    this.loadAuditLogs(0);
  }

  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.currentPage()) {
      return;
    }

    this.loadAuditLogs(page);
  }

  getActionTranslationKey(action: string): string {
    return `MODERATOR.AUDIT_LOGS.ACTIONS.${action}`;
  }

  getTargetTypeTranslationKey(targetType: string): string {
    return `MODERATOR.AUDIT_LOGS.TARGET_TYPES.${targetType}`;
  }

  private loadAuditLogs(page: number): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.auditLogsService.getAuditLogs({ page, size: PAGE_SIZE }).subscribe({
      next: (pageResponse) => {
        this.auditLogs.set(pageResponse.content);
        this.currentPage.set(page);
        this.totalPages.set(pageResponse.totalPages);
        this.totalElements.set(pageResponse.totalElements);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('MODERATOR.AUDIT_LOGS.ERROR_LOAD'));
        this.isLoading.set(false);
      },
    });
  }

  getDetailsTranslationKey(action: string): string {
    return `MODERATOR.AUDIT_LOGS.DETAILS.${action}`;
  }
}
