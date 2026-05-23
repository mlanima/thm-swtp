import { Component, inject, signal } from '@angular/core';
import { LogoComponent } from './logo/logo.component';
import { FeatureComponent } from './feature/feature.component';
import { AuthPanelComponent } from './auth-panel/auth-panel.component';
import { User } from '../types/user.type';
import { SidebarService } from '../sidebar/sidebar.service';

@Component({
  selector: 'app-header',
  imports: [
    LogoComponent,
    // FeatureComponent,
    AuthPanelComponent,
    FeatureComponent,
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent {
  sidebarService = inject(SidebarService);
  private _user = signal<User | null>(null);

  isAuthenticated = () => this.user !== null;

  get user(): User | null {
    return { username: 'test' };
    return this._user();
  }
}
