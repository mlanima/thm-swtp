import { Routes } from '@angular/router';
import {UserProfile} from './feature/user-profile/pages/user-profile/user-profile';
import {Impressum} from './feature/legal-notice/pages/impressum';
import {ContactRequests} from './feature/contact-request/pages/contact-requests/contact-requests';
import { ProjectSite } from './feature/project-site/project-site';
import { ProjectCreate } from './feature/project-create/project-create';
import { SuccessComponent } from './feature/auth/success/success.component';
import {authGuard} from './feature/auth/auth.guard'
import { SearchPage } from './feature/search/pages/search-page/search-page';
import { MyProjectsPage } from './feature/my-projects/pages/my-projects-page/my-projects-page';
import { ProjectSettings } from './feature/project-settings/project-settings';

export const routes: Routes = [
  {path: '', redirectTo: 'impressum', pathMatch: 'full' },
  {path: 'success', component: SuccessComponent},
  {path: 'impressum', component: Impressum},
  {path: 'profiles/:username', component: UserProfile, canActivate: [authGuard]},
  {path: 'contact-requests', component: ContactRequests, canActivate: [authGuard]},
  {path: 'project/:projectUrl', component: ProjectSite, canActivate: [authGuard]},
  {path: 'project/:projectUrl/settings', component: ProjectSettings, canActivate: [authGuard]},
  {path: 'search', component: SearchPage, canActivate: [authGuard]},
  {path: 'createProject', component: ProjectCreate, canActivate: [authGuard]},
  {path: 'my-projects', component: MyProjectsPage, canActivate: [authGuard]},
];