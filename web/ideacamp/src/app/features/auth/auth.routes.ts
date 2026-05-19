import { Routes } from '@angular/router';
import { RegisterUsername } from './pages/register-username/register-username';
import { RegisterPassword } from './pages/register-password/register-password';
import { RegisterMail } from './pages/register-mail/register-mail';
import { RegisterDescription } from './pages/register-description/register-description';
import { RegisterSuccess } from './pages/register-success/register-success';

export const authRoutes: Routes = [
  { path: 'register/username', component: RegisterUsername },
  { path: 'register/password', component: RegisterPassword },
  { path: 'register/mail', component: RegisterMail },
  { path: 'register/description', component: RegisterDescription },
  { path: 'register/success', component: RegisterSuccess },
];
