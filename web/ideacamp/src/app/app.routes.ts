import { Routes } from '@angular/router';
import {UserProfile} from './feature/user-profile/pages/user-profile/user-profile';
import {Impressum} from './feature/legal-notice/pages/impressum';
import {ContactRequests} from './feature/contact-request/pages/contact-requests/contact-requests';
import { ProjectSite } from './pages/project-site/project-site';

export const routes: Routes = [
  {path: 'profile', component: UserProfile},
  {path: 'impressum',component: Impressum},
  {path:'contact-requests', component: ContactRequests},
  {path: 'project',component: ProjectSite}
];
