import {Component, EventEmitter, Output} from '@angular/core';

@Component({
  selector: 'app-project-finish-form',
  imports: [],
  templateUrl: './project-finish-form.html',
})
export class ProjectFinishForm {
  @Output() back = new EventEmitter<void>();
  @Output() finish = new EventEmitter<void>();
}
