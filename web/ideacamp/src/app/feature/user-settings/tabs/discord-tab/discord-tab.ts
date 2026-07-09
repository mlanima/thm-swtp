import { Component, inject, OnInit, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../../auth/auth.service';
import { UserProfileService } from '../../../../services/user-profile.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../enviroments/enviroment.dev';

@Component({
  selector: 'app-discord-tab',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './discord-tab.html',
})
export class DiscordTab implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly http = inject(HttpClient);

  readonly isLoading = signal(true);
  readonly discordUsername = signal<string | null>(null);
  readonly discordId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly isDisconnecting = signal(false);

  ngOnInit(): void {
    this.authService.waitUntilAuthReady().then(() => {
      this.userProfileService.getMyProfile().subscribe({
        next: (profile) => {
          this.discordUsername.set(profile.discordUsername ?? null);
          this.discordId.set(profile.discordId ?? null);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.errorMessage.set('USER_SETTINGS.DISCORD.ERROR_LOAD');
        },
      });
    });
  }

  connect(): void {
    this.isLoading.set(true);
    this.http.get<{ url: string }>(`${environment.apiUrl}/v1/auth/discord/authorize`).subscribe({
      next: (res) => {
        window.location.href = res.url;
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('USER_SETTINGS.DISCORD.ERROR_CONNECT');
      },
    });
  }

  disconnect(): void {
    this.isDisconnecting.set(true);
    this.http.delete(`${environment.apiUrl}/v1/auth/discord/disconnect`).subscribe({
      next: () => {
        this.discordUsername.set(null);
        this.discordId.set(null);
        this.isDisconnecting.set(false);
      },
      error: () => {
        this.errorMessage.set('USER_SETTINGS.DISCORD.ERROR_DISCONNECT');
        this.isDisconnecting.set(false);
      },
    });
  }
}
