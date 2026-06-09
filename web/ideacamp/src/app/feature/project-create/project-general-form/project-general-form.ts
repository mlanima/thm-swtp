import {Component, Output, EventEmitter, Input, OnChanges,SimpleChanges} from '@angular/core';
import {FormsModule} from '@angular/forms';

import {ProjectGeneralData, projectGeneralSchema} from '../schemas/project-create.schema';
import {FormErrors, mapZodErrors} from '../schemas/zod-error.helper';


type GeneralFormFields = 'name' | 'shortDescription' | 'description';

@Component({
  selector: 'app-project-general-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './project-general-form.html',
})

/** First step of the project creation wizard.
 * Gathers general project information and validates input
 * with the {@link projectGeneralSchema}.
 */

export class ProjectGeneralForm implements OnChanges {
  @Input() initialFormData?: Partial<ProjectGeneralData>;
  @Output() next = new EventEmitter<ProjectGeneralData>();


  formData = {name: '', shortDescription: '', description: ''};
  errors: FormErrors<GeneralFormFields> = {};

  ngOnChanges(changes: SimpleChanges) {
    if(changes['initialFormData'] && this.initialFormData){
      this.formData = {
        name : this.initialFormData.name ?? '',
        shortDescription: this.initialFormData.shortDescription ?? '',
        description : this.initialFormData.description ?? ''
      };
    }
  }

  submit(){
    const res = projectGeneralSchema.safeParse(this.formData);
    if(!res.success){
      this.errors = mapZodErrors<GeneralFormFields>(res.error);
      return;
    }

    this.errors = {};
    this.next.emit(res.data)
  }

}
