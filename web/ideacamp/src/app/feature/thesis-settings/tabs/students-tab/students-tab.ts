import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ThesisSettingsService } from '../../services/thesis-settings.service';
import { ThesisSettingsStore } from '../../thesis-settings.store';
import { UserSearchPick } from '../../../../shared/user-search-pick/user-search-pick';
import { UserSearchResult } from '../../../search/models/user-search-result.model';
import { ThesisStudentResponse } from '../../../../models/thesis.model';

interface StudentDisplay {
  id: string;
  name: string;
  email: string;
  initials: string;
  avatarColor: string;
}

@Component({
  selector: 'app-students-tab',
  standalone: true,
  imports: [NgClass, UserSearchPick, TranslatePipe],
  templateUrl: './students-tab.html',
})
export class StudentsTab implements OnInit {
  private readonly store = inject(ThesisSettingsStore);
  private readonly thesisSettingsService = inject(ThesisSettingsService);
  private readonly translateService = inject(TranslateService);

  students = signal<StudentDisplay[]>([]);
  isLoading = signal(false);
  loadErrorMessage = signal<string | null>(null);

  isAdding = signal(false);
  addErrorMessage = signal<string | null>(null);

  studentToRemove = signal<StudentDisplay | null>(null);
  isRemoving = signal(false);
  removeErrorMessage = signal<string | null>(null);

  isPickerOpen = signal(false);

  excludedUserIds = computed(() => {
    const thesis = this.store.thesis();
    if (!thesis) return [];
    return [thesis.supervisorKeycloakId, ...this.students().map((student) => student.id)];
  });

  ngOnInit(): void {
    this.loadStudents();
  }

  openPicker(): void {
    this.addErrorMessage.set(null);
    this.isPickerOpen.set(true);
  }

  closePicker(): void {
    this.isPickerOpen.set(false);
  }

  addStudent(user: UserSearchResult): void {
    const thesisId = this.store.thesis()?.id;
    if (!thesisId || this.isAdding()) return;

    this.isAdding.set(true);
    this.addErrorMessage.set(null);

    this.thesisSettingsService.addStudent(thesisId, user.keycloakId).subscribe({
      next: () => {
        this.loadStudents();
        this.isAdding.set(false);
        this.isPickerOpen.set(false);
      },
      error: (error: HttpErrorResponse) => {
        const key =
          error.status === 422
            ? 'THESISSETTINGS.STUDENTS.ERROR_ADD_INVALID'
            : error.status === 409
              ? 'THESISSETTINGS.STUDENTS.ERROR_ADD_DUPLICATE'
              : 'THESISSETTINGS.STUDENTS.ERROR_ADD_STUDENT';
        this.addErrorMessage.set(this.translateService.instant(key));
        this.isAdding.set(false);
      },
    });
  }

  openRemoveModal(student: StudentDisplay): void {
    this.removeErrorMessage.set(null);
    this.studentToRemove.set(student);
  }

  closeRemoveModal(): void {
    this.studentToRemove.set(null);
  }

  confirmRemove(): void {
    const thesisId = this.store.thesis()?.id;
    const student = this.studentToRemove();
    if (!thesisId || !student || this.isRemoving()) return;

    this.isRemoving.set(true);
    this.removeErrorMessage.set(null);

    this.thesisSettingsService.removeStudent(thesisId, student.id).subscribe({
      next: () => {
        this.loadStudents();
        this.studentToRemove.set(null);
        this.isRemoving.set(false);
      },
      error: (error: HttpErrorResponse) => {
        const key = error.status === 404
          ? 'THESISSETTINGS.STUDENTS.ERROR_REMOVE_NOT_FOUND'
          : 'THESISSETTINGS.STUDENTS.ERROR_REMOVE_STUDENT';
        this.removeErrorMessage.set(this.translateService.instant(key));
        this.isRemoving.set(false);
      },
    });
  }

  private loadStudents(): void {
    const thesisId = this.store.thesis()?.id;
    if (!thesisId) return;

    this.isLoading.set(true);
    this.loadErrorMessage.set(null);

    this.thesisSettingsService.getThesisStudents(thesisId).subscribe({
      next: (students) => {
        this.students.set(students.map((student) => this.toDisplay(student)));
        this.isLoading.set(false);
      },
      error: () => {
        this.loadErrorMessage.set(this.translateService.instant('THESISSETTINGS.STUDENTS.ERROR_LOAD_STUDENTS'));
        this.isLoading.set(false);
      },
    });
  }

  private toDisplay(student: ThesisStudentResponse): StudentDisplay {
    return {
      id: student.keycloakId,
      name: student.username,
      email: student.email,
      initials: student.username.slice(0, 2).toUpperCase(),
      avatarColor: this.getAvatarColor(student.username),
    };
  }

  private getAvatarColor(val: string): string {
    const colors = ['bg-lime-500', 'bg-slate-500', 'bg-rose-500', 'bg-blue-500', 'bg-violet-500'];
    return colors[val.length % colors.length];
  }
}
