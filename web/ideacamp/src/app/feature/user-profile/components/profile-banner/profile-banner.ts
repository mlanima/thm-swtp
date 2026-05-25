import { Component, Input } from '@angular/core';
import { LocationIcon } from '../../../../shared/icons/location-icon/location-icon';
import { FollowersIcon } from '../../../../shared/icons/followers-icon/followers-icon';
import { UserProfileModel } from '../../../../models/user-profile.model';

/** Displays the profile banner of the user
 *
 * The banner receives a complete 'UserProfileModel' and displays profile-related
 * information such as username, location, followers and initials
 */
@Component({
  selector: 'app-profile-banner',
  standalone: true,
  imports: [LocationIcon, FollowersIcon],
  templateUrl: './profile-banner.html',
})
export class ProfileBanner {
  /**
   * Profile data displayed inside the banner
   *
   * This input is required because the banner cannot render meaningful user data without a profile object
   */
  @Input({ required: true }) profile!: UserProfileModel;

  /**
   * First uppercase letter of the username
   *
   * Used as a fallback avatar representation. Returns '?' when no username is available.
   */
  get initials(): string {
    const username = this.profile?.username?.trim();

    if (!username) {
      return '?';
    }

    return username.charAt(0).toUpperCase();
  }
}
