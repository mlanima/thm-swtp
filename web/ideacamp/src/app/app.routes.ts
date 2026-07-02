import { Routes } from '@angular/router';
import {UserProfile} from './feature/user-profile/pages/user-profile/user-profile';
import {Impressum} from './feature/legal-notice/pages/impressum';
import {ContactRequests} from './feature/contact-request/pages/contact-requests/contact-requests';
import { ProjectSite } from './feature/project-site/project-site';
import { ProjectCreate } from './feature/project-create/project-create';
import { SuccessComponent } from './feature/auth/success/success.component';
import {authGuard} from './feature/auth/auth.guard';
import {moderatorGuard} from './feature/auth/moderator.guard';
import { bannedAccountGuard } from './feature/auth/banned-account.guard';
import { SearchPage } from './feature/search/pages/search-page/search-page';
import { MyProjectsPage } from './feature/my-projects/pages/my-projects-page/my-projects-page';
import { ProjectSettings } from './feature/project-settings/project-settings';
import { ThesisSite } from './feature/thesis-site/thesis-site';
import { ThesisSettings } from './feature/thesis-settings/thesis-settings';
import { FavoritesPage } from './feature/favorites/pages/favorites-page/favorites-page';
import { LandingPage } from './feature/landing-page/pages/landing-page/landing-page';
import { UserSettings } from './feature/user-settings/user-settings';
import { ModeratorPage } from './feature/moderator/moderator-page';
import { ProjectsComponent } from './feature/moderator/projects/projects.component';
import { UserManagement } from './feature/moderator/user-management/pages/user-management';
import { BannedAccount } from './feature/banned-account/pages/banned-account';
import { ProfessorRequestComponent } from './feature/moderator/professor-request/professor-request.component';

export const routes: Routes = [
  {path: '', redirectTo: 'landing', pathMatch: 'full' },
  {path: 'success', component: SuccessComponent, canActivate: [authGuard]},
  {path: 'impressum', component: Impressum},
  {path: 'landing', component: LandingPage},
  {path: 'moderator', component: ModeratorPage, canActivate: [moderatorGuard]},
  {path: 'moderator/projects', component: ProjectsComponent, canActivate: [moderatorGuard]},
  {path: 'profiles/:username', component: UserProfile, canActivate: [authGuard]},
  {path: 'contact-requests', component: ContactRequests, canActivate: [authGuard]},
  {path: 'project/:projectUrl', component: ProjectSite, canActivate: [authGuard]},
  {path: 'project/:projectUrl/settings', component: ProjectSettings, canActivate: [authGuard]},
  {path: 'thesis/:thesisUrl', component: ThesisSite, canActivate: [authGuard]},
  {path: 'thesis/:thesisUrl/settings', component: ThesisSettings, canActivate: [authGuard]},
  {path: 'search', component: SearchPage, canActivate: [authGuard]},
  {path: 'createProject', component: ProjectCreate, canActivate: [authGuard]},
  {path: 'my-projects', component: MyProjectsPage, canActivate: [authGuard]},
  {path: 'favorites', component: FavoritesPage, canActivate: [authGuard]},
  {path: 'settings', component: UserSettings, canActivate: [authGuard]},
  {path: 'professor-request', redirectTo: 'settings', pathMatch: 'full'},
  {path: 'moderator/users', component: UserManagement, canActivate: [moderatorGuard]},
  {path: 'account-banned', component: BannedAccount, canActivate: [bannedAccountGuard]},
  {path: 'moderator/professor-requests', component: ProfessorRequestComponent, canActivate: [moderatorGuard]},
];
