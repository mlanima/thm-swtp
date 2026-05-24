import { Component, inject } from '@angular/core';
import { LogoComponent } from './logo/logo.component';
import { FeatureComponent } from './feature/feature.component';
import { AuthPanelComponent } from './auth-panel/auth-panel.component';
import { SidebarService } from '../sidebar/sidebar.service';
import { AuthService } from '../../feature/auth/auth.service';

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
  auth = inject(AuthService);

  isAuthenticated = () => this.auth.isLoggedIn();
}
