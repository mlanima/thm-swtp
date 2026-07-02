import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, inject, OnInit, PLATFORM_ID, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ManagedUser } from '../models/managed-user.model';
import { UserManagementService } from '../service/user-management.service';

type ModTab = 'active' | 'banned';


@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [DatePipe, TranslatePipe],
  templateUrl: './user-management.html',
})
export class UserManagement implements OnInit {
  private readonly userManagementService = inject(UserManagementService);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly maxBanReasonLength = 1000;
  private readonly banReasonPreviewLength = 50;

  activeTab = signal<ModTab>('active');

  activeUsers = signal<ManagedUser[]>([]);
  bannedUsers = signal<ManagedUser[]>([]);
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
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.userManagementService.getUsers('ACTIVE').subscribe({
      next: (response) => {
        this.activeUsers.set(response.content);
        this.activeUserCount.set(response.totalElements);
      },
      error: () => {
        this.errorMessage.set('MODERATOR.USER_MANAGEMENT.ERROR_LOAD');
        this.isLoading.set(false);
      },
      complete: () => {
        this.isLoading.set(false);
      },
    });

    this.userManagementService.getUsers('BANNED').subscribe({
      next: (response) => {
        this.bannedUsers.set(response.content);
        this.bannedUserCount.set(response.totalElements);
      },
      error: () => {
        this.errorMessage.set('MODERATOR.USER_MANAGEMENT.ERROR_LOAD');
      },
    });
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
        this.loadUsers();
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
        this.loadUsers();
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
