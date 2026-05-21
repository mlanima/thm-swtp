import { Component, effect, inject, signal } from '@angular/core';
import { SidebarService } from './sidebar.service';
import { RecentComponent } from './recent/recent.component';
import { MenuLinkComponent } from './menu-link/menu-link.component';

@Component({
  selector: 'app-sidebar',
  imports: [RecentComponent, MenuLinkComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent {
  sidebarService = inject(SidebarService);
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

  onPanelAnimationEnd() {
    if (!this.isClosing()) {
      return;
    }

    this.isRendered.set(false);
    this.isClosing.set(false);
  }
}
