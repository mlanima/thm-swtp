import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ProjectCreateData} from '../schemas/project-create.schema';

@Component({
  selector: 'app-project-finish-form',
  standalone: true,
  imports: [],
  templateUrl: './project-finish-form.html',
})
export class ProjectFinishForm {
  @Input() projectData : Partial<ProjectCreateData> = {};

  @Output() back = new EventEmitter<void>();
  @Output() finish = new EventEmitter<void>();
}
