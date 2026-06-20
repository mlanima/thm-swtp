import { Component, effect, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { SidebarService } from './sidebar.service';
import { MenuLinkComponent } from './menu-link/menu-link.component';
import { AuthService } from '../../feature/auth/auth.service';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectInvitationService } from '../../feature/my-projects/services/project-invitation.service';

@Component({
  selector: 'app-sidebar',
  imports: [MenuLinkComponent, RouterLink, TranslatePipe],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent {
  sidebarService = inject(SidebarService);
  auth = inject(AuthService);
  private readonly invitationService = inject(ProjectInvitationService);
  isRendered = signal(false);
  isClosing = signal(false);
  readonly pendingInvitations = signal(0);

  private readonly platformId = inject(PLATFORM_ID);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.auth.waitUntilAuthReady().then(() => {
        this.invitationService.getInvitations().subscribe({
          next: (invitations) =>
            this.pendingInvitations.set(
              invitations.filter((i) => i.status === 'PENDING').length,
            ),
        });
      });
    }

    effect(() => {
      if (this.sidebarService.isOpen()) {
        this.isRendered.set(true);
        this.isClosing.set(false);
        return;
      }

      if (this.isRendered()) {
        this.isClosing.set(true);
      }
    });
  }

  requestClose() {
    this.sidebarService.close();
  }

  logout() {
    this.requestClose();
    this.auth.logout();
  }

  onPanelAnimationEnd() {
    if (!this.isClosing()) {
      return;
    }

    this.isRendered.set(false);
    this.isClosing.set(false);
  }
}
