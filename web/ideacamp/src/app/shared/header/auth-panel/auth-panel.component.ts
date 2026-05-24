import { Component, inject, input } from '@angular/core';
import { User } from '../../types/user.type';
import { NgOptimizedImage } from '@angular/common';
import { AuthService } from '../../../feature/auth/auth.service';

@Component({
  selector: 'app-auth-panel',
  imports: [NgOptimizedImage],
  templateUrl: './auth-panel.component.html',
  styleUrl: './auth-panel.component.css',
})
export class AuthPanelComponent {
  user = input.required<User>();
  private auth = inject(AuthService);

  get username(): string {
    return this.user().username;
  }

  readonly imageUrl: string | null = null;

  logout(): void {
    this.auth.logout();
  }
}

