import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LocationIcon } from '../../../../shared/icons/location-icon/location-icon';
import { FollowersIcon } from '../../../../shared/icons/followers-icon/followers-icon';
import { UserProfileModel } from '../../../../models/user-profile.model';
import { EditButton } from '../../../../shared/edit-button/edit-button';
import { TranslatePipe } from '@ngx-translate/core';

/** Displays the profile banner of the user
 *
 * The banner receives a complete 'UserProfileModel' and displays profile-related
 * information such as username, location, followers and initials
 */
@Component({
  selector: 'app-profile-banner',
  standalone: true,
  imports: [LocationIcon, FollowersIcon, EditButton, FormsModule, TranslatePipe],
  templateUrl: './profile-banner.html',
})
export class ProfileBanner {
  /**
   * Profile data displayed inside the banner
   *
   * This input is required because the banner cannot render meaningful user data without a profile object
   */
  @Input({ required: true }) profile!: UserProfileModel;

  /** Whether the current viewer is the profile owner and may edit */
  @Input() isOwnProfile = false;

  @Input() isEditing = false;

  @Input() isSaving = false;

  @Input() editForm!: {
    title: string;
    location: string;
    about: string;
    experience: string;
  };

  /** Emits when the edit button inside the banner is clicked */
  @Output() edit = new EventEmitter<void>();

  @Output() save = new EventEmitter<void>();

  @Output() cancelEdit = new EventEmitter<void>();

  /**
   * First uppercase letter of the username
   *
   * Used as a fallback avatar representation
   *
   * @returns '?' when no username is available
   */
  get initials(): string {
    const username = this.profile?.username?.trim();

    if (!username) {
      return '?';
    }

    return username.charAt(0).toUpperCase();
  }
}
