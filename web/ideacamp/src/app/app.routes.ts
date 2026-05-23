import { Routes } from '@angular/router';
import { SearchProject } from './pages/search-project/search-project';
import {UserProfile} from './feature/user-profile/pages/user-profile/user-profile';
import {Impressum} from './feature/legal-notice/pages/impressum';
import {ContactRequests} from './feature/contact-request/pages/contact-requests/contact-requests';

export const routes: Routes = [
  {path: 'profile', component: UserProfile},
  {path: 'impressum',component: Impressum},
  {path:'contact-requests', component: ContactRequests},
  {path: 'searchProject', component: SearchProject}
];
