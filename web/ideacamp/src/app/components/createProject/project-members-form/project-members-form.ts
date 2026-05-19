import {Component, EventEmitter, Output} from '@angular/core';

@Component({
  selector: 'app-project-members-form',
  imports: [],
  templateUrl: './project-members-form.html',
})
export class ProjectMembersForm {
  @Output() next = new EventEmitter<void>();
  @Output() back = new EventEmitter<void>();
}
