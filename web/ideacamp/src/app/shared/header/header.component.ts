import { Component, signal } from '@angular/core';
import { LogoComponent } from './logo/logo.component';
import { FeatureComponent } from './feature/feature.component';
import { AuthPanelComponent } from './auth-panel/auth-panel.component';
import { User } from '../types/user.type';

@Component({
  selector: 'app-header',
  imports: [
    LogoComponent,
    // FeatureComponent,
    AuthPanelComponent
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent {
  private _user = signal<User | null>(null);


  isAuthenticated = () => this.user !== null;

  get user(): User | null {
    return { username: 'test'};
    return this._user();
  }

}
