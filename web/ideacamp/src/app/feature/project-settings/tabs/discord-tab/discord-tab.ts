import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../enviroments/enviroment.dev';
import { ProjectSettingsStore } from '../../project-settings.store';

interface ChannelStatus {
  isActive: boolean;
  channelId: string | null;
  syncedToday: number;
}

interface ChannelSettings {
  notifyPostCreated: boolean;
  notifyPostUpdated: boolean;
  notifyPostDeleted: boolean;
  notifyMemberJoin: boolean;
  notifyMemberLeave: boolean;
}

@Component({
  selector: 'app-discord-tab',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './discord-tab.html',
})
export class DiscordTab implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly store = inject(ProjectSettingsStore);
  private readonly translate = inject(TranslateService);

  readonly channelId = signal('');
  readonly isConnecting = signal(false);
  readonly isDisconnecting = signal(false);
  readonly isLoading = signal(true);
  readonly connectError = signal<string | null>(null);

  readonly connectionStatus = signal<ChannelStatus | null>(null);
  readonly settings = signal<ChannelSettings | null>(null);

  ngOnInit(): void {
    const projectId = this.store.project()?.id;
    if (!projectId) return;
    this.loadStatus();
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
      next: (res) => {
        this.connectionStatus.set({ isActive: res.isActive, channelId: res.discordChannelId, syncedToday: 0 });
        this.loadSettings();
        this.isConnecting.set(false);
      },
      error: (err) => {
        this.connectError.set(err.error?.message || 'PROJECTSETTINGS.DISCORD.ERROR_CONNECT');
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
        this.settings.set(null);
        this.isDisconnecting.set(false);
      },
      error: () => {
        this.isDisconnecting.set(false);
      },
    });
  }

  toggleSetting(key: keyof ChannelSettings): void {
    const current = this.settings();
    if (!current) return;

    const updated = { ...current, [key]: !current[key] };
    const projectId = this.store.project()?.id;
    if (!projectId) return;

    this.http.put<ChannelSettings>(
      `${environment.apiUrl}/v1/projects/${projectId}/discord/settings`,
      updated,
    ).subscribe({
      next: (res) => this.settings.set(res),
    });
  }

  private loadStatus(): void {
    const projectId = this.store.project()?.id;
    if (!projectId) return;

    this.http.get<ChannelStatus>(`${environment.apiUrl}/v1/projects/${projectId}/discord/connect`).subscribe({
      next: (status) => {
        this.connectionStatus.set(status);
        this.channelId.set(status.channelId ?? '');
        if (status.isActive) {
          this.loadSettings();
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  private loadSettings(): void {
    const projectId = this.store.project()?.id;
    if (!projectId) return;

    this.http.get<ChannelSettings>(`${environment.apiUrl}/v1/projects/${projectId}/discord/settings`).subscribe({
      next: (s) => this.settings.set(s),
    });
  }
}
