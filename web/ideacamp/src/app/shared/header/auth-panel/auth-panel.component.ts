import { Component, input } from '@angular/core';
import { User } from '../../types/user.type';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-auth-panel',
  imports: [NgOptimizedImage],
  templateUrl: './auth-panel.component.html',
  styleUrl: './auth-panel.component.css',
})
export class AuthPanelComponent {
  user = input.required<User>();

  get username() {
    return this.user().username;
  }

  get imageUrl() {
    return null;
  }
}

