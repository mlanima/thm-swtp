import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  ModeratorProfRequest,
  ProfRequestStatus,
} from './models/moderator-professor-request.model';
import { ModeratorProfessorRequestService } from './service/moderator-professor-request.service'
import { Pagination } from '../shared/pagination/pagination';

type ProfRequestAction = 'accept' | 'reject';
const PAGE_SIZE = 20;

@Component({
  selector: 'app-professor-request',
  standalone: true,
  imports: [DatePipe, TranslatePipe, Pagination],
  templateUrl: './professor-request.component.html',
  styleUrl: './professor-request.component.css',
})
export class ProfessorRequestComponent implements OnInit {
  private readonly professorRequestService = inject(ModeratorProfessorRequestService);
  private readonly translateService = inject(TranslateService);

  readonly requests = signal<ModeratorProfRequest[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');

  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly selectedRequest = signal<ModeratorProfRequest | null>(null);
  readonly selectedAction = signal<ProfRequestAction | null>(null);
  readonly isSubmitting = signal(false);
  readonly actionError = signal('');

  readonly confirmTitle = computed(() => {
    return this.selectedAction() === 'accept'
      ? 'MODERATOR.PROFESSOR_REQUESTS.CONFIRM_ACCEPT_TEXT'
      : 'MODERATOR.PROFESSOR_REQUESTS.CONFIRM_REJECT_TITLE';
  });

  readonly confirmText = computed(() => {
    return this.selectedAction() === 'accept'
      ? 'MODERATOR.PROFESSOR_REQUESTS.CONFIRM_ACCEPT_TEXT'
      : 'MODERATOR.PROFESSOR_REQUESTS.CONFIRM_REJECT_TEXT';
  });

  ngOnInit(): void {
    this.loadRequests(0);
  }

  loadRequests(page: number): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.professorRequestService.getRequests(page, PAGE_SIZE).subscribe({
      next: (pageResponse) => {
        this.requests.set(pageResponse.content);
        this.currentPage.set(pageResponse.number);
        this.totalPages.set(pageResponse.totalPages);
        this.totalElements.set(pageResponse.totalElements);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(
          this.translateService.instant('MODERATOR.PROFESSOR_REQUESTS.ERROR_LOAD'),
        );
        this.isLoading.set(false);
      },
    });
  }

  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.currentPage()) {
      return;
    }
    this.loadRequests(page);
  }

  openConfirmDialog(request: ModeratorProfRequest, action: ProfRequestAction): void {
    this.selectedRequest.set(request);
    this.selectedAction.set(action);
    this.actionError.set('');
  }

  closeConfirmDialog(): void {
    this.selectedRequest.set(null);
    this.selectedAction.set(null);
    this.isSubmitting.set(false);
    this.actionError.set('');
  }

  confirmAction(): void {
    const req = this.selectedRequest();
    const action = this.selectedAction();

    if (!req || !action || this.isSubmitting()) {
      return;
    }
    this.isLoading.set(true);
    this.actionError.set('');

    const actionReq =
      action === 'accept'
        ? this.professorRequestService.acceptRequest(req.id)
        : this.professorRequestService.rejectRequest(req.id);

    actionReq.subscribe({
      next: () => {
        this.closeConfirmDialog();
        this.loadRequests(this.currentPage());
      },
      error: () => {
        const actionType =
          action === 'accept'
            ? 'MODERATOR.PROFESSOR_REQUESTS.ERROR_ACCEPT'
            : 'MODERATOR.PROFESSOR_REQUESTS.ERROR_REJECT';

        this.actionError.set(this.translateService.instant(actionType));
        this.isLoading.set(false);
      },
    });
  }

  getStatusTranslationKey(status: ProfRequestStatus): string {
    return `MODERATOR.PROFESSOR_REQUESTS.STATUS_${status}`;
  }

  getStatusClasses(status: ProfRequestStatus): string {
    switch (status) {
      case 'PENDING':
        return 'bg-amber-100 text-amber-700 ring-amber-200';
      case 'ACCEPTED':
        return 'bg-emerald-100 text-emerald-700 ring-emerald-200';
      case 'REJECTED':
        return 'bg-red-100 text-red-700 ring-red-200';
    }
  }
}
