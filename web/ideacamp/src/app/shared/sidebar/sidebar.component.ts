import { Component, effect, inject, signal } from '@angular/core';
import { SidebarService } from './sidebar.service';
import { MenuLinkComponent } from './menu-link/menu-link.component';
import { AuthService } from '../../feature/auth/auth.service';

@Component({
  selector: 'app-sidebar',
  imports: [MenuLinkComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent {
  sidebarService = inject(SidebarService);
  auth = inject(AuthService);
  isRendered = signal(false);
  isClosing = signal(false);

  constructor() {
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
