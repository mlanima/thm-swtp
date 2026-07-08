import { Component, computed, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ThesisSettingsService } from '../../../thesis-settings/services/thesis-settings.service';
import { ThesisStudentResponse } from '../../../../models/thesis.model';

interface PersonDisplay {
  id: string;
  name: string;
  initials: string;
  avatarColor: string;
  isSupervisor: boolean;
}

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [NgClass, RouterLink, TranslatePipe],
  templateUrl: './student-list.html',
})
export class StudentList implements OnChanges {
  private readonly thesisSettingsService = inject(ThesisSettingsService);
  private readonly translateService = inject(TranslateService);

  @Input({ required: true }) thesisId!: string;
  @Input() supervisorId = '';
  @Input() supervisorUsername = '';

  people = signal<PersonDisplay[]>([]);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showModal = signal(false);

  openModal(): void { this.showModal.set(true); }
  closeModal(): void { this.showModal.set(false); }

  readonly hasMore = computed(() => this.people().length > 4);
  readonly visiblePeople = computed(() => this.hasMore() ? this.people().slice(0, 3) : this.people());
  readonly extraPeople = computed(() => this.people().slice(3));
  readonly extraCount = computed(() => this.people().length - 3);

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['thesisId'] || changes['supervisorId'] || changes['supervisorUsername']) && this.thesisId) {
      this.loadStudents();
    }
  }

  private loadStudents(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.thesisSettingsService.getThesisStudents(this.thesisId).subscribe({
      next: (students) => {
        const mapped = students.map((s) => this.toDisplay(s));
        if (this.supervisorUsername && this.supervisorId) {
          const supervisorAlreadyPresent = mapped.some((p) => p.id === this.supervisorId);
          if (!supervisorAlreadyPresent) {
            mapped.unshift({
              id: this.supervisorId,
              name: this.supervisorUsername,
              initials: this.supervisorUsername.slice(0, 2).toUpperCase(),
              avatarColor: this.getAvatarColor(this.supervisorUsername),
              isSupervisor: true,
            });
          }
        }
        this.people.set(mapped);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('THESISSETTINGS.STUDENTS.ERROR_LOAD_STUDENTS'));
        this.isLoading.set(false);
      },
    });
  }

  private toDisplay(student: ThesisStudentResponse): PersonDisplay {
    return {
      id: student.keycloakId,
      name: student.username,
      initials: student.username.slice(0, 2).toUpperCase(),
      avatarColor: this.getAvatarColor(student.username),
      isSupervisor: student.keycloakId === this.supervisorId,
    };
  }

  private getAvatarColor(val: string): string {
    const colors = ['bg-lime-500', 'bg-slate-500', 'bg-rose-500', 'bg-blue-500', 'bg-violet-500'];
    return colors[val.length % colors.length];
  }
}
