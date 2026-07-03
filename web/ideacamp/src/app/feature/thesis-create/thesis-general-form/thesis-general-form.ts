import {Component, Output, EventEmitter, Input, OnChanges, SimpleChanges} from '@angular/core';
import {FormsModule} from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {ThesisGeneralData, thesisGeneralSchema} from '../schemas/thesis-create.schema';
import {FormErrors, mapZodErrors} from '../../project-create/schemas/zod-error.helper';


type GeneralFormFields = 'title' | 'shortDescription' | 'description';

@Component({
  selector: 'app-thesis-general-form',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './thesis-general-form.html',
})

/** First step of the thesis creation wizard.
 * Gathers general thesis information and validates input
 * with the {@link thesisGeneralSchema}.
 */

export class ThesisGeneralForm implements OnChanges {
  @Input() initialFormData?: Partial<ThesisGeneralData>;
  @Output() next = new EventEmitter<ThesisGeneralData>();


  formData = {title: '', shortDescription: '', description: ''};
  errors: FormErrors<GeneralFormFields> = {};

  ngOnChanges(changes: SimpleChanges) {
    if(changes['initialFormData'] && this.initialFormData){
      this.formData = {
        title : this.initialFormData.title ?? '',
        shortDescription: this.initialFormData.shortDescription ?? '',
        description : this.initialFormData.description ?? ''
      };
    }
  }

  submit(){
    const res = thesisGeneralSchema.safeParse(this.formData);
    if(!res.success){
      this.errors = mapZodErrors<GeneralFormFields>(res.error);
      return;
    }

    this.errors = {};
    this.next.emit(res.data)
  }

  focusShortDescriptionIfTitleIsValid(shortDescriptionInput: HTMLInputElement) {
    const res = thesisGeneralSchema.pick({ title: true }).safeParse({
      title: this.formData.title,
    });

    if (!res.success) {
      this.errors = {
        ...this.errors,
        ...mapZodErrors<GeneralFormFields>(res.error),
      };
      return;
    }

    this.errors = {
      ...this.errors,
      title: undefined,
    };

    shortDescriptionInput.focus();
  }
}
