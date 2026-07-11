import { Component, effect, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { SidebarService } from './sidebar.service';
import { MenuLinkComponent } from './menu-link/menu-link.component';
import { AuthService } from '../../feature/auth/auth.service';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectInvitationService } from '../../feature/my-projects/services/project-invitation.service';
import { UserProfileService } from '../../services/user-profile.service';
import { MyThesesService } from '../../feature/my-theses/services/my-theses.service';
import { ThesisNotificationService } from '../../feature/my-theses/services/thesis-notification.service';

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
  private readonly userProfileService = inject(UserProfileService);
  private readonly myThesesService = inject(MyThesesService);
  private readonly thesisNotificationService = inject(ThesisNotificationService);
  isRendered = signal(false);
  isClosing = signal(false);
  isProfessor = signal(false);
  hasTheses = signal(false);

  private readonly platformId = inject(PLATFORM_ID);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.auth.waitUntilAuthReady().then(() => {
        if (this.auth.isModerator()) {
          return;
        }
        this.invitationService.getInvitations().subscribe({
          next: (invitations) =>
            this.sidebarService.pendingInvitationsCount.set(
              invitations.filter((i) => i.status === 'PENDING').length,
            ),
        });
        this.userProfileService.getMyProfile().subscribe({
          next: (profile) => {
            this.isProfessor.set(profile.isProfessor);
            if (profile.isProfessor) {
              return;
            }
            const username = this.auth.username();
            if (!username) {
              return;
            }
            this.myThesesService.getMyTheses(username).subscribe({
              next: (theses) => this.hasTheses.set(theses.length > 0),
            });
            this.thesisNotificationService.getUnreadCount().subscribe({
              next: (response) => this.sidebarService.unreadThesisNotificationsCount.set(response.count),
            });
          },
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
