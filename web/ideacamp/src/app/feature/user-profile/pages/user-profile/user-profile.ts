import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, OnDestroy, inject, PLATFORM_ID, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ProfileInformation } from '../../components/profile-information/profile-information';
import { ProfileBanner } from '../../components/profile-banner/profile-banner';
import { UserProfileService } from '../../../../services/user-profile.service';
import { AuthService } from '../../../auth/auth.service';
import { UserProfileModel } from '../../../../models/user-profile.model';
import { ProfileTagListComponent } from '../../components/profile-tag-list/profile-tag-list.component'
import { SuccessModal } from '../../../../shared/success-modal/success-modal';

import { LinkManagerComponent } from '../../../../shared/link-manager/link-manager';
import { LinkManagerDataSource } from '../../../../shared/link-manager/link-manager.types';
import { UserProfileLinkModel } from '../../../../models/user-profile-link.model';
import { UserProfileLinkService } from '../../services/user-profile-link.service';

interface ProfileViewState {
  /** Indicates whether the profile request is currently running. */
  isLoading: boolean;

  /** Contains the loaded user profile or null if no profile is available. */
  profile: UserProfileModel | null;

  /** Contains an error message if loading or saving the profile failed. */
  errorMessage: string;
}

/** Displays a user profile. Shows edit controls only when the viewer is the profile owner. */
@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [
    ProfileInformation,
    ProfileBanner,
    FormsModule,
    ProfileTagListComponent,
    SuccessModal,
    TranslatePipe,
    LinkManagerComponent,
  ],
  templateUrl: './user-profile.html',
})
export class UserProfile implements OnInit, OnDestroy {
  /** Platform identifier used to check whether the component runs in the browser. */
  private readonly platformId = inject(PLATFORM_ID);

  /** Route used to read the :username parameter. */
  private readonly route = inject(ActivatedRoute);

  /** Service used to load and update user profile data from the backend. */
  private readonly userProfileService = inject(UserProfileService);

  /** Service used to load / update / delete link data from the backend. */
  private readonly userProfileLinkService = inject(UserProfileLinkService);

  /** Data source for profile links. Initialized after the profile has been loaded. */
  profileLinkDataSource: LinkManagerDataSource<UserProfileLinkModel> | null = null;

  /** Auth service used to determine whether the viewer is the profile owner. */
  private readonly authService = inject(AuthService);

  private readonly translateService = inject(TranslateService);

  /** Username read from the route parameter :username. */
  routeUsername = '';

  private paramSub: Subscription | null = null;

  /** Reactive view state used by the template for loading, success and error states */
  readonly profileState = signal<ProfileViewState>({
    isLoading: true,
    profile: null,
    errorMessage: '',
  });

  /** True when the logged-in user is viewing their own profile. */
  get isOwner(): boolean {
    return this.authService.username() === this.routeUsername;
  }

  /** Currently edited inline profile section */
  editingSection: 'banner' | 'about' | 'experience' | null = null;

  /** Indicates whether a save request is currently running */
  isSaving = false;

  /** Controls the visibility of the success modal */
  showSuccessModal = false;

  /** Form state containing all editable profile fields */
  editForm = {
    title: '',
    location: '',
    about: '',
    experience: '',
  };

  /**
   * Initializes profile loading when the component runs in the browser.
   * Waits for auth to be ready so the isOwner check is reliable before fetching.
   *
   * Server-side rendering does not send an authenticated profile request
   */
  async ngOnInit(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      this.profileState.set({
        isLoading: false,
        profile: null,
        errorMessage: '',
      });

      return;
    }

    await this.authService.waitUntilAuthReady();

    this.paramSub = this.route.paramMap.subscribe((params) => {
      this.routeUsername = params.get('username') ?? '';

      if (!this.routeUsername) {
        this.profileState.set({
          isLoading: false,
          profile: null,
          errorMessage: this.translateService.instant('USERPROFILE.ERROR_NOT_FOUND'),
        });
        return;
      }

      this.loadProfile();
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  /**
   * Loads the profile by username from the route parameter.
   *
   * Updates the reactive profile state with loading, success or error data
   */
  private loadProfile(): void {
    this.profileState.set({
      isLoading: true,
      profile: null,
      errorMessage: '',
    });

    this.userProfileService.getProfile(this.routeUsername).subscribe({
      next: (profile) => {
        this.profileLinkDataSource = this.createProfileLinkDataSource(profile.keycloakId);
        this.profileState.set({
          isLoading: false,
          profile,
          errorMessage: '',
        });
      },
      error: (error) => {
        const errorMessage =
          error.status === 401 || error.status === 403
            ? this.translateService.instant('USERPROFILE.ERROR_AUTH_REQUIRED')
            : error.status === 404
              ? this.translateService.instant('USERPROFILE.ERROR_NOT_FOUND')
              : this.translateService.instant('USERPROFILE.ERROR_LOAD_PROFILE');

        this.profileState.set({
          isLoading: false,
          profile: null,
          errorMessage,
        });
      },
    });
  }

  /**
   * Starts inline editing for the selected profile section
   *
   * @param profile Profile data used to prefill the edit form
   * @param section Profile section that should be edited
   */
  startEditing(profile: UserProfileModel, section: 'banner' | 'about' | 'experience'): void {
    this.editForm = {
      title: profile.title ?? '',
      location: profile.location ?? '',
      about: profile.about ?? '',
      experience: profile.experience ?? '',
    };

    this.editingSection = section;
  }

  /** Cancels inline editing and returns to the normal profile view */
  cancelEditing(): void {
    this.editingSection = null;
  }

  /**
   * Saves the current edit form through the backend update endpoint
   * On success the local profile state is replaced with the updated profile
   *
   * @param profile Existing profile used for username and error fallback state
   */
  saveProfile(profile: UserProfileModel): void {
    this.isSaving = true;

    this.userProfileService
      .updateProfile(profile.username, {
        title: this.editForm.title,
        location: this.editForm.location,
        about: this.editForm.about,
        experience: this.editForm.experience,
      })
      .subscribe({
        next: (updatedProfile) => {
          this.profileState.set({
            isLoading: false,
            profile: updatedProfile,
            errorMessage: '',
          });

          this.editingSection = null;
          this.isSaving = false;
          this.showSuccessModal = true;
        },
        error: (error) => {
          const errorMessage =
            error.status === 401 || error.status === 403
              ? this.translateService.instant('USERPROFILE.ERROR_EDIT_FORBIDDEN')
              : this.translateService.instant('USERPROFILE.ERROR_UPDATE_PROFILE');

          this.profileState.set({
            isLoading: false,
            profile,
            errorMessage,
          });

          this.isSaving = false;
        },
      });
  }

  /** Closes the success modal after a successful profile update */
  closeSuccessModal(): void {
    this.showSuccessModal = false;
  }

  private createProfileLinkDataSource(userId: string): LinkManagerDataSource<UserProfileLinkModel> {
    return {
      load: () => this.userProfileLinkService.getUserProfileLinks(userId),
      createLink: (request) => this.userProfileLinkService.addUserProfileLink(userId, request),
      updateLink: (linkId, request) =>
        this.userProfileLinkService.updateUserProfileLink(userId, linkId, request),
      deleteLink: (linkId) => this.userProfileLinkService.deleteUserProfileLink(userId, linkId),
    };
  }
}
