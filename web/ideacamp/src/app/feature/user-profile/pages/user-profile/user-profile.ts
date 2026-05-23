import { Component } from '@angular/core';
import {ProfileInformation} from '../../components/profile-information/profile-information'
import {ProfileBanner} from '../../components/profile-banner/profile-banner'

/** Displays public user profile information.*/
@Component({
  selector: 'app-user-profile',
  standalone : true,
  imports: [ProfileInformation, ProfileBanner],
  templateUrl: './user-profile.html',
})
export class UserProfile {
  aboutText = "Passionate software engineer with over 5 years of experience in building scalable web applications. I love working with modern technologies and contributing to open-source projects. Currently focused on full-stack development with React, Node.js, and cloud technologies. When I'm not coding, you can find me exploring new technologies, attending tech meetups, or sharing knowledge through blog posts and conference talks.";
  experienceText = "Passionate software engineer with over 5 years of experience in building scalable web applications. I love working with modern technologies and contributing to open-source projects. Currently focused on full-stack development with React, Node.js, and cloud technologies.";
}
