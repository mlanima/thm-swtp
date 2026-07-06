import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../enviroments/enviroment.dev';
import { ProjectSettingsStore } from '../../project-settings.store';
import { UserProfileService } from '../../../../services/user-profile.service';

interface ChannelStatus {
  isActive: boolean;
  channelId: string | null;
  discordInviteUrl?: string | null;
  syncedToday: number;
}

@Component({
  selector: 'app-discord-tab',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './discord-tab.html',
})
export class DiscordTab implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly store = inject(ProjectSettingsStore);
  private readonly userProfileService = inject(UserProfileService);

  readonly channelId = signal('');
  readonly isConnecting = signal(false);
  readonly isDisconnecting = signal(false);
  readonly isLoading = signal(true);
  readonly connectError = signal<string | null>(null);

  readonly connectionStatus = signal<ChannelStatus | null>(null);
  readonly inviteUrl = signal('');
  readonly isUpdatingInvite = signal(false);
  readonly isFetchingBotInvite = signal(false);
  readonly isAutoConnecting = signal(false);

  readonly isDiscordLinked = signal(false);
  readonly isUserLoading = signal(true);
  readonly linkError = signal<string | null>(null);
  readonly isLinkingDiscord = signal(false);

  ngOnInit(): void {
    const projectId = this.store.project()?.id;
    if (!projectId) return;
    this.loadUserDiscordStatus();
    this.loadStatus();
    window.addEventListener('focus', this.onFocus);
  }

  ngOnDestroy(): void {
    window.removeEventListener('focus', this.onFocus);
  }

  private readonly onFocus = (): void => {
    const projectId = this.store.project()?.id;
    if (!projectId) return;
    if (this.connectionStatus()?.isActive || this.isAutoConnecting()) return;
    const pending = localStorage.getItem('discord-pending-setup');
    if (pending === projectId) {
      localStorage.removeItem('discord-pending-setup');
      this.isLoading.set(false);
      this.autoConnect();
    }
  };

  linkDiscord(): void {
    this.isLinkingDiscord.set(true);
    this.linkError.set(null);
    this.http.get<{ url: string }>(`${environment.apiUrl}/v1/auth/discord/authorize`).subscribe({
      next: (res) => {
        window.location.href = res.url;
      },
      error: () => {
        this.isLinkingDiscord.set(false);
        this.linkError.set('PROJECTSETTINGS.DISCORD.ERROR_LINK');
      },
    });
  }

  private loadUserDiscordStatus(): void {
    this.userProfileService.getMyProfile().subscribe({
      next: (profile) => {
        this.isDiscordLinked.set(!!profile.discordId);
        this.isUserLoading.set(false);
      },
      error: () => {
        this.isUserLoading.set(false);
      },
    });
  }

  addBotToServer(): void {
    const projectId = this.store.project()?.id;
    if (!projectId) return;

    const win = window.open('', '_blank');
    this.isFetchingBotInvite.set(true);
    this.http.get<{ url: string }>(`${environment.apiUrl}/v1/projects/${projectId}/discord/bot-invite`).subscribe({
      next: (res) => {
        localStorage.setItem('discord-pending-setup', projectId);
        this.isFetchingBotInvite.set(false);
        if (win) {
          win.location.href = res.url;
        } else {
          window.location.href = res.url;
        }
      },
      error: () => {
        this.isFetchingBotInvite.set(false);
      },
    });
  }

  testAndConnect(): void {
    const projectId = this.store.project()?.id;
    if (!projectId || !this.channelId().trim()) return;

    this.isConnecting.set(true);
    this.connectError.set(null);

    this.http.post<{ id: string; discordChannelId: string; isActive: boolean }>(
      `${environment.apiUrl}/v1/projects/${projectId}/discord/connect`,
      { channelId: this.channelId().trim() },
    ).subscribe({
      next: (res: any) => {
        this.connectionStatus.set({ isActive: res.isActive, channelId: res.discordChannelId, discordInviteUrl: res.discordInviteUrl, syncedToday: 0 });
        this.inviteUrl.set(res.discordInviteUrl ?? '');
        this.isConnecting.set(false);
      },
      error: (err) => {
        this.connectError.set(this.errorKey(err));
        this.isConnecting.set(false);
      },
    });
  }

  disconnect(): void {
    const projectId = this.store.project()?.id;
    if (!projectId) return;

    this.isDisconnecting.set(true);
    this.http.delete(`${environment.apiUrl}/v1/projects/${projectId}/discord/connect`).subscribe({
      next: () => {
        this.connectionStatus.set(null);
        this.isDisconnecting.set(false);
      },
      error: () => {
        this.isDisconnecting.set(false);
      },
    });
  }

  updateInviteUrl(): void {
    const projectId = this.store.project()?.id;
    if (!projectId) return;

    this.isUpdatingInvite.set(true);
    this.http.patch<{ discordInviteUrl: string }>(
      `${environment.apiUrl}/v1/projects/${projectId}/discord/invite`,
      { discordInviteUrl: this.inviteUrl().trim() || null },
    ).subscribe({
      next: (res) => {
        this.connectionStatus.update(s => s ? { ...s, discordInviteUrl: res.discordInviteUrl } : null);
        this.isUpdatingInvite.set(false);
      },
      error: () => {
        this.isUpdatingInvite.set(false);
      },
    });
  }

  private autoConnect(): void {
    const projectId = this.store.project()?.id;
    if (!projectId) return;

    this.isAutoConnecting.set(true);
    this.connectError.set(null);

    this.http.post<{ id: string; discordChannelId: string; isActive: boolean; discordInviteUrl?: string }>(
      `${environment.apiUrl}/v1/projects/${projectId}/discord/auto-connect`,
      {},
    ).subscribe({
      next: (res) => {
        this.connectionStatus.set({ isActive: res.isActive, channelId: res.discordChannelId, discordInviteUrl: res.discordInviteUrl, syncedToday: 0 });
        this.inviteUrl.set(res.discordInviteUrl ?? '');
        this.isAutoConnecting.set(false);
      },
      error: (err) => {
        this.connectError.set(this.errorKey(err));
        this.isAutoConnecting.set(false);
      },
    });
  }

  private loadStatus(): void {
    const projectId = this.store.project()?.id;
    if (!projectId) return;

    this.http.get<any>(`${environment.apiUrl}/v1/projects/${projectId}/discord/connect`).subscribe({
      next: (status) => {
        localStorage.removeItem('discord-pending-setup');
        this.connectionStatus.set({ isActive: status.isActive, channelId: status.discordChannelId, discordInviteUrl: status.discordInviteUrl, syncedToday: 0 });
        this.channelId.set(status.discordChannelId ?? '');
        this.inviteUrl.set(status.discordInviteUrl ?? '');
        this.isLoading.set(false);
      },
      error: () => {
        const pending = localStorage.getItem('discord-pending-setup');
        if (pending === projectId) {
          localStorage.removeItem('discord-pending-setup');
          this.isLoading.set(false);
          this.autoConnect();
        } else {
          this.isLoading.set(false);
        }
      },
    });
  }

  private errorKey(err: any): string {
    const msg = err.error?.message;
    if (msg && msg.includes('link your Discord account')) {
      return 'PROJECTSETTINGS.DISCORD.NEEDS_DISCORD_LINK';
    }
    return msg || 'PROJECTSETTINGS.DISCORD.ERROR_CONNECT';
  }
}
