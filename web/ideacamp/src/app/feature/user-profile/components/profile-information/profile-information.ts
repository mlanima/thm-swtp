import { Component, Input, Output, EventEmitter } from '@angular/core';
import {EditButton} from '../../../../shared/edit-button/edit-button';

/** Stores public user profile information.*/
@Component({
  selector: 'app-profile-information',
  standalone: true,
  imports: [EditButton],
  templateUrl: './profile-information.html',
})
export class ProfileInformation {
  @Input() title = '';
  @Input() text = '';
  @Input() isOwnProfile = false;

  @Output() edit = new EventEmitter<void>();
}
