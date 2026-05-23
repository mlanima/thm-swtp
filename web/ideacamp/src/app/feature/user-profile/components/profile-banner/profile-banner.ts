import { Component } from '@angular/core';
import {LocationIcon} from '../../../../shared/icons/location-icon/location-icon'
import {FollowersIcon} from '../../../../shared/icons/followers-icon/followers-icon'

/** Displays the profile banner of the user.*/
@Component({
  selector: 'app-profile-banner',
  standalone : true,
  imports: [LocationIcon, FollowersIcon],
  templateUrl: './profile-banner.html',
})


export class ProfileBanner {

  dummy = {
    initials : 'JD',
    username : 'John Doe',
    title : 'Software Engineer',
    location : 'Gießen',
    followers: '14'
  };

}
