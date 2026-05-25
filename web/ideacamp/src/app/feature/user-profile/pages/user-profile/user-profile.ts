import { isPlatformBrowser, AsyncPipe } from '@angular/common';
import { Component, OnInit, inject, PLATFORM_ID } from '@angular/core';
import { Observable, defer, of } from 'rxjs';
import { catchError, map, startWith } from 'rxjs';

import { ProfileInformation } from '../../components/profile-information/profile-information';
import { ProfileBanner } from '../../components/profile-banner/profile-banner';
import { UserProfileService } from '../../../../services/user-profile.service';
import { UserProfileModel } from '../../../../models/user-profile.model';

interface ProfileViewState {
  /** Indicates whether the profile request is currently running. */
  isLoading: boolean;

  /** Contains the loaded user profile or 'null' if no profile is available. */
  profile: UserProfileModel | null;

  /** Contains a error message if loading the profile failed. */
  errorMessage: string;
}

/** Displays public user profile information.
 *
 * The component loads the profile from the backend only in the browser
 * This avoids running authenticated HTTP requests during server-side rendering
 *
 * The template consumes the profile state through 'profileState$', which contains loading, success and error states
 */
@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [ProfileInformation, ProfileBanner, AsyncPipe],
  templateUrl: './user-profile.html',
})
export class UserProfile implements OnInit {
  /** Platform identifier used to check whether the component runs in the browser */
  private readonly platformId = inject(PLATFORM_ID);

  /** Service used to load user profile data from the backend */
  private readonly userProfileService = inject(UserProfileService);

  /**
   * Observable view state for the profile page
   *
   * Emits loading, success and error states so the template can react accordingly
   */
  profileState$!: Observable<ProfileViewState>;


  /**
   * Initializes profile loading after the component has been created
   *
   * If the component is rendered outside the browser, no backend request is sent
   * In the browser, the current user's profile is loaded from the backend and mapped into a view state for the template
   */
  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.profileState$ = of({
        isLoading: false,
        profile: null,
        errorMessage: '',
      });

      return;
    }

    this.profileState$ = defer(() => {

      return this.userProfileService.getMyProfile().pipe(
        map((profile) => ({
          isLoading: false,
          profile,
          errorMessage: '',
        })),
        catchError((error) => {
          const errorMessage =
            error.status === 401 || error.status === 403
              ? 'Please log in to view the Profile'
              : 'Profile information could not be loaded. Please try again later.';

          return of({
            isLoading: false,
            profile: null,
            errorMessage,
          });
        }),
        startWith({
          isLoading: true,
          profile: null,
          errorMessage: '',
        }),
      );
    });
  }
}
