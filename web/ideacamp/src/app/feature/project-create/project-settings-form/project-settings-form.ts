import {Component, Output, EventEmitter, Input, OnChanges,SimpleChanges} from '@angular/core';
import {FormsModule} from '@angular/forms';

import {ProjectSettingsData, projectSettingsSchema} from '../schemas/project-create.schema';
import {FormErrors, mapZodErrors} from '../schemas/zod-error.helper';

type SettingsFormFields = 'projectUrl' | 'isPrivateProject';

@Component({
  selector: 'app-project-settings-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './project-settings-form.html',
})

/** Second step of the project creation wizard.
 * Gathers the project setting information and validates input
 * with the {@link projectSettingsSchema}
 */

export class ProjectSettingsForm implements OnChanges {
  @Input() initialFormData?: Partial<ProjectSettingsData>;
  @Output() next = new EventEmitter<ProjectSettingsData>();
  @Output() back = new EventEmitter<ProjectSettingsData>();

  formData = {projectUrl: '', isPrivateProject: false};
  errors: FormErrors<SettingsFormFields> = {};

  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialFormData'] && this.initialFormData) {
      this.formData = {
        projectUrl: this.initialFormData.projectUrl ?? '',
        isPrivateProject: this.initialFormData.isPrivateProject ?? false,
      };
    }
  }

  togglePrivateProject() {
    this.formData.isPrivateProject = !this.formData.isPrivateProject;
  }

  submit(){
    const res = projectSettingsSchema.safeParse(this.formData);

    if(!res.success){
      this.errors = mapZodErrors<SettingsFormFields>(res.error);
      return;
    }

    this.errors = {};
    this.next.emit(res.data);
  }

  goBack(){
    this.back.emit(this.formData);
  }

}
