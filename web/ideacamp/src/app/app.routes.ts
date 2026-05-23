import { Routes } from '@angular/router';
import {ContactRequests} from './pages/contact-requests/contact-requests';

export const routes: Routes = [
  {path:'contact-requests', component: ContactRequests},
    {
    path: 'impressum',
    component: Impressum
  }
];
