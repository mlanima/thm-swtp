import {Component, Output, EventEmitter} from '@angular/core';

@Component({
  selector: 'app-project-general-form',
  standalone: true,
  templateUrl: './project-general-form.html',
})
export class ProjectGeneralForm {
  @Output() next = new EventEmitter<void>();
}
