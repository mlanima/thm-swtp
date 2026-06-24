import { DatePipe } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

type ModTab = 'active' | 'banned';

interface ModUser {
  id: string;
  name: string;
  email: string;
  role: 'Mod' | 'Member';
  joinedAt: Date;
  initials: string;
  banned: boolean;
  bannedAt?: Date;
  bannedBy?: string;
  banReason?: string;
}

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [DatePipe, TranslatePipe],
  templateUrl: './user-management.html'
})
export class UserManagement {
  private readonly maxBanReasonLength = 250;
  private readonly banReasonPreviewLength = 50;

  activeTab = signal<ModTab>('active');

  users = signal<ModUser[]>([
    {
      id: '1',
      name: 'Felix Wagner',
      email: 'f.wagner@thm.de',
      role: 'Member',
      joinedAt: new Date(2026, 2, 3),
      initials: 'FW',
      banned: false,
    },
  ]);

  selectedUser = signal<ModUser | null>(null);
  banReason = signal('');

  activeUsers = computed(() => this.users().filter(user => !user.banned));
  bannedUsers = computed(() => this.users().filter(user => user.banned));

  openBanDialog(user: ModUser): void {
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

    const trimmedReason = this.banReason()
      .trim()
      .slice(0, this.maxBanReasonLength);

    this.users.update(users =>
      users.map(currentUser =>
        currentUser.id === user.id
          ? {
            ...currentUser,
            banned: true,
            bannedAt: new Date(),
            bannedBy: 'Mod',
            banReason: trimmedReason || undefined,
          }
          : currentUser,
      ),
    );

    this.closeBanDialog();
    this.activeTab.set('banned');
  }

  unbanUser(user: ModUser): void {
    this.users.update(users =>
      users.map(currentUser =>
        currentUser.id === user.id
          ? {
            ...currentUser,
            banned: false,
            bannedAt: undefined,
            bannedBy: undefined,
            banReason: undefined,
          }
          : currentUser,
      ),
    );

    this.activeTab.set('active');
  }

  getBanReasonPreview(reason?: string): string | undefined {
    if (!reason) {
      return undefined;
    }

    return reason.length > this.banReasonPreviewLength
      ? `${reason.slice(0, this.banReasonPreviewLength)}...`
      : reason;
  }
}
