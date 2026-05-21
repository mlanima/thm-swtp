import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  emailOrName = '';
  password = '';

  submitted = false;
  loading = false;

  onSubmit() {
    this.submitted = true;

    if (!this.emailOrName || !this.password) {
      return;
    }

    this.loading = true;
  }
}
