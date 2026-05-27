import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import {z} from 'zod';
import {WizardLayout} from './wizard-layout/wizard-layout';
import {Stepper} from './stepper/stepper';

import {ProjectGeneralForm} from './project-general-form/project-general-form';
import {ProjectSettingsForm} from './project-settings-form/project-settings-form';
import {ProjectMembersForm} from './project-members-form/project-members-form';
import {ProjectFinishForm} from './project-finish-form/project-finish-form';
import { ProjectService } from '../project-site/project.service';

import {ProjectGeneralData, ProjectSettingsData, ProjectCreateData, projectCreateSchema} from './schemas/project-create.schema';

@Component({
  selector: 'app-project-create',
  standalone: true,
  imports: [
    WizardLayout,
    Stepper,
    ProjectGeneralForm,
    ProjectSettingsForm,
    ProjectMembersForm,
    ProjectFinishForm,
  ],
  templateUrl: './project-create.html',
})
export class ProjectCreate {

  private readonly projectService = inject(ProjectService);
  private readonly router = inject(Router);

  isLoading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  currentStep = 0;

  /** 'projectData' is used to store already collected project data across all wizard steps.
   * 'Partial' must be used, because the project data is filled in step by step
   */
  projectData: Partial<ProjectCreateData> = {};

  nextStep() {
    if (this.currentStep < 3) {
      this.currentStep++;
    }
  }
  previousStep() {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  /** Saves the validated general-form data and goes to the next wizard step.*/
  saveGeneralFormAndContinue(data: ProjectGeneralData) {
    this.projectData = { ...this.projectData, ...data };
    this.nextStep();
  }

  /** Saves the validated settings-form data and goes to the next wizard step.*/
  saveSettingsFormAndContinue(data: ProjectSettingsData) {
    this.projectData = { ...this.projectData, ...data };
    this.nextStep();
  }

  /** Saves the validated settings-form data and goes back to the previous wizard step.*/
  saveSettingsFormAndBack(data: ProjectSettingsData) {
    this.projectData = { ...this.projectData, ...data };
    this.previousStep();
  }
  /** Validates the collected project data before creating the project.*/
  finishProjectCreation() {
    const res = projectCreateSchema.safeParse(this.projectData);
    if (!res.success) {
      console.log('Project validation failed:', z.treeifyError(res.error));
      return;
    }
    this.isLoading = true;
    this.errorMessage = null;

    this.projectService.createProject({ ...res.data, memberIds: [], tagIds: [] }).subscribe({
      next: (project) => {
        this.isLoading = false;
        this.successMessage = 'Projekt erfolgreich erstellt!';
        setTimeout(() => {
          this.router.navigate(['/project', project.id]);
        }, 1500);
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Projekt konnte nicht erstellt werden. Bitte versuche es erneut.';
      },
    });
  }
}
