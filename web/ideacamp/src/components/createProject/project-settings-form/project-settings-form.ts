import {Component, EventEmitter, Output} from '@angular/core';

@Component({
  selector: 'app-project-settings-form',
  imports: [],
  templateUrl: './project-settings-form.html',
})
export class ProjectSettingsForm {

  @Output() next = new EventEmitter<void>();
  @Output() back = new EventEmitter<void>();

  isPrivateProject = false;

  togglePrivateProject() {
    this.isPrivateProject = !this.isPrivateProject;
  }
}
