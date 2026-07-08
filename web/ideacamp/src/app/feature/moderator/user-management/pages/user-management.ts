import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, inject, OnInit, PLATFORM_ID, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ManagedUser, ManagedUserSortField, SortDirection } from '../models/managed-user.model';
import { UserManagementService } from '../service/user-management.service';
import { Pagination } from '../../shared/pagination/pagination';

type ModTab = 'active' | 'banned';
const PAGE_SIZE = 10;


@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [DatePipe, TranslatePipe, Pagination],
  templateUrl: './user-management.html',
})
export class UserManagement implements OnInit {
  private readonly userManagementService = inject(UserManagementService);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly maxBanReasonLength = 1000;
  private readonly banReasonPreviewLength = 50;

  activeTab = signal<ModTab>('active');

  activeUsers = signal<ManagedUser[]>([]);
  activeCurrentPage = signal(0);
  activeTotalPages = signal(0);
  activeSortField = signal<ManagedUserSortField>('username');
  activeSortDirection = signal<SortDirection>('asc');

  bannedUsers = signal<ManagedUser[]>([]);
  bannedCurrentPage = signal(0);
  bannedTotalPages = signal(0);
  bannedSortField = signal<ManagedUserSortField>('username');
  bannedSortDirection = signal<SortDirection>('asc');

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  selectedUser = signal<ManagedUser | null>(null);
  banReason = signal('');

  activeUserCount = signal(0);
  bannedUserCount = signal(0);

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.loadUsers();
  }

  loadUsers(): void {
    this.loadActiveUsers(this.activeCurrentPage());
    this.loadBannedUsers(this.bannedCurrentPage());
  }

  loadActiveUsers(page: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.userManagementService
      .getUsers('ACTIVE', page, PAGE_SIZE, this.activeSortField(), this.activeSortDirection())
      .subscribe({
        next: (response) => {
          this.activeUsers.set(response.content);
          this.activeUserCount.set(response.totalElements);
          this.activeCurrentPage.set(response.number);
          this.activeTotalPages.set(response.totalPages);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set('MODERATOR.USER_MANAGEMENT.ERROR_LOAD');
          this.isLoading.set(false);
        },
      });
  }

  loadBannedUsers(page: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.userManagementService
      .getUsers('BANNED', page, PAGE_SIZE, this.bannedSortField(), this.bannedSortDirection())
      .subscribe({
        next: (response) => {
          this.bannedUsers.set(response.content);
          this.bannedUserCount.set(response.totalElements);
          this.bannedCurrentPage.set(response.number);
          this.bannedTotalPages.set(response.totalPages);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set('MODERATOR.USER_MANAGEMENT.ERROR_LOAD');
          this.isLoading.set(false);
        },
      });
  }

  onActivePageChange(page: number): void {
    if (page < 0 || page >= this.activeTotalPages() || page === this.activeCurrentPage()) {
      return;
    }
    this.loadActiveUsers(page);
  }

  onBannedPageChange(page: number): void {
    if (page < 0 || page >= this.bannedTotalPages() || page === this.bannedCurrentPage()) {
      return;
    }

    this.loadBannedUsers(page);
  }

  changeActiveSort(field: ManagedUserSortField): void {
    if (this.activeSortField() === field) {
      this.activeSortDirection.set(this.activeSortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.activeSortField.set(field);
      this.activeSortDirection.set('asc');
    }

    this.loadActiveUsers(0);
  }

  changeBannedSort(field: ManagedUserSortField): void {
    if (this.bannedSortField() === field) {
      this.bannedSortDirection.set(this.bannedSortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.bannedSortField.set(field);
      this.bannedSortDirection.set('asc');
    }

    this.loadBannedUsers(0);
  }

  getActiveSortIndicator(field: ManagedUserSortField): string {
    if (this.activeSortField() !== field) {
      return '↕';
    }

    return this.activeSortDirection() === 'asc' ? '↑' : '↓';
  }

  getBannedSortIndicator(field: ManagedUserSortField): string {
    if (this.bannedSortField() !== field) {
      return '↕';
    }

    return this.bannedSortDirection() === 'asc' ? '↑' : '↓';
  }

  getActiveSortTitle(field: ManagedUserSortField): string {
    if (this.activeSortField() !== field) {
      return 'MODERATOR.USER_MANAGEMENT.SORT.NOT_SORTED';
    }

    return this.activeSortDirection() === 'asc'
      ? 'MODERATOR.USER_MANAGEMENT.SORT.ASC'
      : 'MODERATOR.USER_MANAGEMENT.SORT.DESC';
  }

  getBannedSortTitle(field: ManagedUserSortField): string {
    if (this.bannedSortField() !== field) {
      return 'MODERATOR.USER_MANAGEMENT.SORT.NOT_SORTED';
    }

    return this.bannedSortDirection() === 'asc'
      ? 'MODERATOR.USER_MANAGEMENT.SORT.ASC'
      : 'MODERATOR.USER_MANAGEMENT.SORT.DESC';
  }

  openBanDialog(user: ManagedUser): void {
    this.selectedUser.set(user);
    this.banReason.set('');
  }

  closeBanDialog(): void {
    this.selectedUser.set(null);
    this.banReason.set('');
  }

  banUser(): void {
    const user = this.selectedUser();

    if (!user) {
      return;
    }

    const trimmedReason = this.banReason().trim().slice(0, this.maxBanReasonLength);

    this.userManagementService.banUser(user.keycloakId, trimmedReason || undefined).subscribe({
      next: () => {
        this.closeBanDialog();
        this.activeTab.set('banned');
        this.loadActiveUsers(this.activeCurrentPage());
        this.loadBannedUsers(0);
      },
      error: () => {
        this.errorMessage.set('MODERATOR.USER_MANAGEMENT.ERROR_BAN');
      },
    });
  }

  unbanUser(user: ManagedUser): void {
    this.userManagementService.unbanUser(user.keycloakId).subscribe({
      next: () => {
        this.activeTab.set('active');
        this.loadBannedUsers(this.bannedCurrentPage());
        this.loadActiveUsers(0);
      },
      error: () => {
        this.errorMessage.set('MODERATOR.USER_MANAGEMENT.ERROR_UNBAN');
      },
    });
  }

  getBanReasonPreview(reason?: string | null | undefined): string | undefined {
    if (!reason) {
      return undefined;
    }

    return reason.length > this.banReasonPreviewLength
      ? `${reason.slice(0, this.banReasonPreviewLength)}...`
      : reason;
  }

  getInitials(username: string): string {
    return username
      .split(/[.\s_-]+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  }

  getRoleTranslationKey(user: ManagedUser): string {
    return user.isProfessor
      ? 'MODERATOR.USER_MANAGEMENT.ROLES.PROFESSOR'
      : 'MODERATOR.USER_MANAGEMENT.ROLES.MEMBER';
  }
}
