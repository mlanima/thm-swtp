import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, OnDestroy, inject, PLATFORM_ID, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ProfileInformation } from '../../components/profile-information/profile-information';
import { ProfileBanner } from '../../components/profile-banner/profile-banner';
import { UserProfileService, UpdateUserProfileRequest } from '../../../../services/user-profile.service';
import { AuthService } from '../../../auth/auth.service';
import { UserProfileModel } from '../../../../models/user-profile.model';
import { ToastService } from '../../../../shared/toast/toast.service';
import { ProfileTagListComponent } from '../../components/profile-tag-list/profile-tag-list.component'
import { SuccessModal } from '../../../../shared/success-modal/success-modal';

import { LinkManagerComponent } from '../../../../shared/link-manager/link-manager';
import { LinkManagerDataSource } from '../../../../shared/link-manager/link-manager.types';
import { UserProfileLinkModel } from '../../../../models/user-profile-link.model';
import { UserProfileLinkService } from '../../services/user-profile-link.service';

interface ProfileViewState {
  isLoading: boolean;
  profile: UserProfileModel | null;
  errorMessage: string;
}

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
  private readonly platformId = inject(PLATFORM_ID);

  private readonly route = inject(ActivatedRoute);

  private readonly userProfileService = inject(UserProfileService);

  private readonly userProfileLinkService = inject(UserProfileLinkService);

  private readonly toastService = inject(ToastService);

  profileLinkDataSource: LinkManagerDataSource<UserProfileLinkModel> | null = null;

  private readonly authService = inject(AuthService);

  private readonly translateService = inject(TranslateService);

  routeUsername = '';

  private paramSub: Subscription | null = null;

  readonly profileState = signal<ProfileViewState>({
    isLoading: true,
    profile: null,
    errorMessage: '',
  });

  get isOwner(): boolean {
    return this.authService.username() === this.routeUsername;
  }

  editingSection: 'banner' | 'about' | 'experience' | null = null;

  isSaving = false;

  showSuccessModal = false;

  editForm: {
    title: string;
    location: string;
    about: string;
    experience: string;
    placeId: string;
  } = {
    title: '',
    location: '',
    about: '',
    experience: '',
    placeId: '',
  };

  private initialEditValues: typeof this.editForm | null = null;

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

  startEditing(profile: UserProfileModel, section: 'banner' | 'about' | 'experience'): void {
    this.editForm = {
      title: profile.title ?? '',
      location: profile.location ?? '',
      about: profile.about ?? '',
      experience: profile.experience ?? '',
      placeId: profile.placeId ?? '',
    };

    this.initialEditValues = { ...this.editForm };
    this.editingSection = section;
  }

  cancelEditing(): void {
    this.editingSection = null;
  }

  saveProfile(profile: UserProfileModel): void {
    this.isSaving = true;

    const body: UpdateUserProfileRequest = {};
    const initial = this.initialEditValues;

    if (initial) {
      if (this.editForm.title !== initial.title) {
        body.title = this.editForm.title;
      }
      if (this.editForm.about !== initial.about) {
        body.about = this.editForm.about;
      }
      if (this.editForm.experience !== initial.experience) {
        body.experience = this.editForm.experience;
      }
      if (this.editForm.location !== initial.location || this.editForm.placeId !== initial.placeId) {
        body.location = this.editForm.location;
        body.placeId = this.editForm.placeId;
      }
    }

    this.userProfileService
      .updateProfile(profile.username, body)
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
          const errorCode = error.error?.errorCode;
          const errorMessage =
            error.status === 401 || error.status === 403
              ? this.translateService.instant('USERPROFILE.ERROR_EDIT_FORBIDDEN')
              : errorCode === 'INVALID_PLACE'
                ? this.translateService.instant('USERPROFILE.ERROR_INVALID_PLACE')
                : errorCode === 'CONTENT_NOT_VALID'
                  ? this.translateService.instant('USERPROFILE.ERROR_CONTENT_NOT_VALID')
                  : this.translateService.instant('USERPROFILE.ERROR_UPDATE_PROFILE');

          this.toastService.error(errorMessage);
          this.isSaving = false;
        },
      });
  }

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
