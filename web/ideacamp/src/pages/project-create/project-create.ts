import { Component } from '@angular/core';
import {WizardLayout} from '../../components/createProject/wizzard-layout/wizard-layout';
import {Stepper} from '../../components/createProject/stepper/stepper';
import {ProjectGeneralForm} from '../../components/createProject/project-general-form/project-general-form';
import {ProjectSettingsForm} from '../../components/createProject/project-settings-form/project-settings-form';
import {ProjectMembersForm} from '../../components/createProject/project-members-form/project-members-form';
import {ProjectFinishForm} from '../../components/createProject/project-finish-form/project-finish-form';

@Component({
  selector: 'app-project-create',
  standalone: true,
  imports: [WizardLayout, Stepper, ProjectGeneralForm, ProjectSettingsForm, ProjectMembersForm, ProjectFinishForm],
  templateUrl: './project-create.html',
})
export class ProjectCreate {

  currentStep = 0;

  nextStep(){
    if(this.currentStep < 3){
      this.currentStep++;
    }
  }
  previousStep(){
    if(this.currentStep > 0){
      this.currentStep--;
    }
  }
  finishStep(){
    console.log("Project creating finished.");
  }
}
