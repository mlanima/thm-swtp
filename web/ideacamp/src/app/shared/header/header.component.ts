import { Component, inject, signal } from '@angular/core';
import { LogoComponent } from './logo/logo.component';
import { FeatureComponent } from './feature/feature.component';
import { AuthPanelComponent } from './auth-panel/auth-panel.component';
import { SidebarService } from '../sidebar/sidebar.service';
import { AuthService } from '../../feature/auth/auth.service';
import { RouterLink } from '@angular/router';
import { LanguageService, AppLanguage } from '../../services/language.service';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-header',
  imports: [
    LogoComponent,
    // FeatureComponent,
    AuthPanelComponent,
    FeatureComponent,
    RouterLink,
    TranslatePipe
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent {
  sidebarService = inject(SidebarService);
  auth = inject(AuthService);

  isAuthenticated = () => this.auth.isLoggedIn();

  private readonly languageService = inject(LanguageService);

  currentLanguage = signal<AppLanguage>(this.languageService.getCurrentLanguage());

  changeLanguage(language: AppLanguage): void {
    this.languageService.changeLanguage(language);
    this.currentLanguage.set(language);
  }
}
