import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, inject, PLATFORM_ID, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ProfileInformation } from '../../components/profile-information/profile-information';
import { ProfileBanner } from '../../components/profile-banner/profile-banner';
import { UserProfileService } from '../../../../services/user-profile.service';
import { UserProfileModel } from '../../../../models/user-profile.model';

interface ProfileViewState {
  /** Indicates whether the profile request is currently running. */
  isLoading: boolean;

  /** Contains the loaded user profile or null if no profile is available. */
  profile: UserProfileModel | null;

  /** Contains an error message if loading or saving the profile failed. */
  errorMessage: string;
}

/** Displays public user profile information. */
@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [ProfileInformation, ProfileBanner, FormsModule],
  templateUrl: './user-profile.html',
})
export class UserProfile implements OnInit {
  /** Platform identifier used to check whether the component runs in the browser. */
  private readonly platformId = inject(PLATFORM_ID);

  /** Service used to load and update user profile data from the backend. */
  private readonly userProfileService = inject(UserProfileService);

  /** Reactive view state used by the template for loading, success and error states */
  readonly profileState = signal<ProfileViewState>({
    isLoading: true,
    profile: null,
    errorMessage: '',
  });

  /** Currently edited inline profile section */
  editingSection: 'banner' | 'about' | 'experience' | null = null;

  /** Indicates whether a save request is currently running */
  isSaving = false;

  /** Controls the visibility of the success modal */
  showSuccessModal = false;

  /** Controls the visibility of the banner edit modal */
  isBannerModalOpen = false;

  /** Selected banner field that should be edited in the modal */
  selectedBannerField: 'title' | 'location' = 'title';

  /** Temporary input value used by the banner edit modal */
  bannerEditValue = '';

  /** Form state containing all editable profile fields */
  editForm = {
    title: '',
    location: '',
    about: '',
    experience: '',
  };

  /**
   * Initializes profile loading when the component runs in the browser
   *
   * Server-side rendering does not send an authenticated profile request
   */
  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.profileState.set({
        isLoading: false,
        profile: null,
        errorMessage: '',
      });

      return;
    }

    this.loadProfile();
  }

  /**
   * Loads the authenticated user's profile from the backend
   *
   * Updates the reactive profile state with loading, success or error data
   */
  private loadProfile(): void {
    this.profileState.set({
      isLoading: true,
      profile: null,
      errorMessage: '',
    });

    this.userProfileService.getMyProfile().subscribe({
      next: (profile) => {
        this.profileState.set({
          isLoading: false,
          profile,
          errorMessage: '',
        });
      },
      error: (error) => {
        const errorMessage =
          error.status === 401 || error.status === 403
            ? 'Please log in to view the Profile'
            : 'Profile information could not be loaded. Please try again later.';

        this.profileState.set({
          isLoading: false,
          profile: null,
          errorMessage,
        });
      },
    });
  }

  /**
   * Opens the banner edit modal and initializes the form with current profile values
   *
   * @param profile Profile data used to prefill the editable fields
   */
  openBannerEditModal(profile: UserProfileModel): void {
    this.editForm = {
      title: profile.title ?? '',
      location: profile.location ?? '',
      about: profile.about ?? '',
      experience: profile.experience ?? '',
    };

    this.selectedBannerField = 'title';
    this.bannerEditValue = this.editForm.title;
    this.isBannerModalOpen = true;
  }

  /**
   * Closes the banner edit modal when no save request is running
   *
   * Keeps the modal open while saving to avoid interrupting an active request
   */
  closeBannerEditModal(): void {
    if (this.isSaving) {
      return;
    }

    this.isBannerModalOpen = false;
    this.bannerEditValue = '';
  }

  /**
   * Updates the modal input value when the selected banner field changes
   *
   * Reads either the current title or location value from the edit form
   */
  onBannerFieldChange(): void {
    this.bannerEditValue =
      this.selectedBannerField === 'title'
        ? this.editForm.title
        : this.editForm.location;
  }

  /**
   * Copies the selected banner field value into the edit form and saves the profile
   *
   * @param profile Profile that should be updated
   */
  saveBannerField(profile: UserProfileModel): void {
    if (this.selectedBannerField === 'title') {
      this.editForm.title = this.bannerEditValue;
    } else {
      this.editForm.location = this.bannerEditValue;
    }

    this.saveProfile(profile);
  }

  /**
   * Starts inline editing for the selected profile section
   *
   * @param profile Profile data used to prefill the edit form
   * @param section Profile section that should be edited
   */
  startEditing(profile: UserProfileModel, section: 'about' | 'experience'): void {
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
          this.isBannerModalOpen = false;
          this.bannerEditValue = '';
          this.isSaving = false;
          this.showSuccessModal = true;
        },
        error: (error) => {
          const errorMessage =
            error.status === 401 || error.status === 403
              ? 'You are not authorized to edit this profile.'
              : 'Profile could not be updated. Please try again later.';

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
}
