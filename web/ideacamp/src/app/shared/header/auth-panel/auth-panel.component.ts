import { Component, inject, input } from '@angular/core';
import { User } from '../../types/user.type';
import { NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../feature/auth/auth.service';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-auth-panel',
  imports: [NgOptimizedImage, RouterLink, TranslatePipe],
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

