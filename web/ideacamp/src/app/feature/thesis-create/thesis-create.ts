import { Component, HostListener, ViewChild, inject } from '@angular/core';
import { Router } from '@angular/router';
import {z} from 'zod';
import {forkJoin, Observable, of} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {WizardLayout} from '../project-create/wizard-layout/wizard-layout';
import {Stepper} from '../project-create/stepper/stepper';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {ThesisGeneralForm} from './thesis-general-form/thesis-general-form';
import {ThesisSettingsForm} from './thesis-settings-form/thesis-settings-form';
import {ThesisStudentsForm} from './thesis-students-form/thesis-students-form';
import {ThesisFinishForm} from './thesis-finish-form/thesis-finish-form';
import { ThesisService } from '../thesis-site/thesis.service';
import { ThesisSettingsService } from '../thesis-settings/services/thesis-settings.service';

import {ThesisGeneralData, ThesisSettingsData, ThesisCreateData, thesisCreateSchema} from './schemas/thesis-create.schema';
import {generateProjectUrl} from '../project-create/project-url.utils';
import {ProjectInviteMember } from '../../models/project-invite-member.model';

const THESIS_STEPS = [
  'THESISCREATE.STEPS.GENERAL',
  'THESISCREATE.STEPS.SETTINGS',
  'THESISCREATE.STEPS.STUDENTS',
  'THESISCREATE.STEPS.FINISH',
];

@Component({
  selector: 'app-thesis-create',
  standalone: true,
  imports: [
    WizardLayout,
    Stepper,
    ThesisGeneralForm,
    ThesisSettingsForm,
    ThesisStudentsForm,
    ThesisFinishForm,
    TranslatePipe
  ],
  templateUrl: './thesis-create.html',
})
export class ThesisCreate {

  private readonly thesisService = inject(ThesisService);
  private readonly thesisSettingsService = inject(ThesisSettingsService);
  private readonly router = inject(Router);
  private readonly translateService = inject(TranslateService);

  readonly steps = THESIS_STEPS;

  @ViewChild(ThesisStudentsForm) studentsForm?: ThesisStudentsForm;

  isLoading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  currentStep = 0;

  /** 'thesisData' is used to store already collected thesis data across all wizard steps.
   * 'Partial' must be used, because the thesis data is filled in step by step
   */
  thesisData: Partial<ThesisCreateData> = {};
  invitedStudents: ProjectInviteMember[] = [];

  @HostListener('window:keydown', ['$event'])
  handleEnter(event: KeyboardEvent) {
    if (event.key !== 'Enter') {
      return;
    }

    if (this.currentStep === 2) {
      if (this.studentsForm?.isStudentDialogOpen) {
        return;
      }

      event.preventDefault();
      this.studentsForm?.submit();
      return;
    }

    if (this.currentStep === 3) {
      if (this.isLoading) {
        return;
      }

      event.preventDefault();
      this.finishThesisCreation();
    }
  }

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
  saveGeneralFormAndContinue(data: ThesisGeneralData) {
    const oldTitle = this.thesisData.title;
    const currentUrl = this.thesisData.thesisUrl ?? '';
    if (oldTitle && data.title !== oldTitle && currentUrl === generateProjectUrl(oldTitle)) {
      this.thesisData = { ...this.thesisData, ...data, thesisUrl: '' };
    } else {
      this.thesisData = { ...this.thesisData, ...data };
    }

    this.nextStep();
  }

  /** Saves the validated settings-form data and goes to the next wizard step.*/
  saveSettingsFormAndContinue(data: ThesisSettingsData) {
    this.thesisData = { ...this.thesisData, ...data };
    this.nextStep();
  }

  /** Saves the validated settings-form data and goes back to the previous wizard step.*/
  saveSettingsFormAndBack(data: ThesisSettingsData) {
    this.thesisData = { ...this.thesisData, ...data };
    this.previousStep();
  }

  /** Saves the added students from the students-form and goes to the next wizard step.*/
  saveStudentsAndContinue(students: ProjectInviteMember[]) {
    this.invitedStudents = students;
    this.nextStep();
  }

  /** Saves the added students from the students-form and goes back to the previous wizard step.*/
  saveStudentsAndBack(students: ProjectInviteMember[]) {
    this.invitedStudents = students;
    this.previousStep();
  }

  /** Validates the collected thesis data before creating the thesis.*/
  finishThesisCreation() {
    const res = thesisCreateSchema.safeParse(this.thesisData);
    if (!res.success) {
      console.log('Thesis validation failed:', z.treeifyError(res.error));
      return;
    }
    this.isLoading = true;
    this.errorMessage = null;

    this.thesisService.createThesis({
      title: res.data.title,
      shortDescription: res.data.shortDescription ?? null,
      description: res.data.description ?? null,
      thesisUrl: res.data.thesisUrl,
      tags: [],
    }).subscribe({
      next: (thesis) => {
        this.addInvitedStudents(thesis.id).subscribe((failedCount) => {
          this.isLoading = false;
          this.successMessage = this.translateService.instant(this.resolveSuccessMessageKey(failedCount));
          setTimeout(() => {
            this.router.navigate(['/thesis', thesis.thesisUrl]);
          }, 1500);
        });
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = this.translateService.instant('THESISCREATE.ERROR_CREATE_THESIS');
      },
    });
  }
  
  private addInvitedStudents(thesisId: string): Observable<number> {
    if (this.invitedStudents.length === 0) {
      return of(0);
    }

    return forkJoin(
      this.invitedStudents.map((student) =>
        this.thesisSettingsService.addStudent(thesisId, student.keycloakId).pipe(
          map(() => true),
          catchError(() => of(false)),
        ),
      ),
    ).pipe(map((results) => results.filter((succeeded) => !succeeded).length));
  }

  private resolveSuccessMessageKey(failedStudentCount: number): string {
    if (this.invitedStudents.length === 0) {
      return 'THESISCREATE.SUCCESS_CREATED';
    }
    if (failedStudentCount === 0) {
      return 'THESISCREATE.SUCCESS_CREATED_WITH_STUDENTS';
    }
    if (failedStudentCount === this.invitedStudents.length) {
      return 'THESISCREATE.SUCCESS_CREATED_STUDENTS_FAILED';
    }
    return 'THESISCREATE.SUCCESS_CREATED_STUDENTS_PARTIAL';
  }
}
