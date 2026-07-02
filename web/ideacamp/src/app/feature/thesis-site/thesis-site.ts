import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ThesisService } from './thesis.service';
import { ThesisResponse } from '../../models/thesis.model';
import { ThesisHeader } from './components/thesis-header/thesis-header';
import { InfoCard } from './components/info-card/info-card';
import { ThesisSidebar } from './components/thesis-sidebar/thesis-sidebar';
import { AuthService } from '../auth/auth.service';
import { SuccessModal } from '../../shared/success-modal/success-modal';

@Component({
  selector: 'app-thesis-site',
  standalone: true,
  imports: [ThesisHeader, InfoCard, ThesisSidebar, FormsModule, CommonModule, SuccessModal, TranslatePipe],
  templateUrl: './thesis-site.html',
})
export class ThesisSite implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly thesisService = inject(ThesisService);
  private readonly authService = inject(AuthService);
  private readonly translateService = inject(TranslateService);

  thesis = signal<ThesisResponse | null>(null);
  errorMessage = signal<string | null>(null);
  isLoading = signal(true);

  isEditing = signal(false);
  isSaving = signal(false);
  showSuccessModal = signal(false);
  editTitle = signal('');
  editShortDescription = signal('');
  editDescription = signal('');

  isSavingTags = signal(false);
  tagsErrorMessage = signal<string | null>(null);

  get isSupervisor(): boolean {
    const user = this.authService.user();
    const thesis = this.thesis();
    if (!user || !thesis) return false;
    return user.id === thesis.supervisorKeycloakId;
  }

  ngOnInit(): void {
    const thesisUrl = this.route.snapshot.paramMap.get('thesisUrl');
    if (!thesisUrl) {
      this.errorMessage.set('THESISSITE.ERRORS.NO_THESIS_URL');
      this.isLoading.set(false);
      return;
    }

    this.thesisService.getThesisByUrl(thesisUrl).subscribe({
      next: (data) => {
        this.thesis.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('THESISSITE.ERRORS.LOAD_THESIS');
        this.isLoading.set(false);
      },
    });
  }

  startEdit(): void {
    const thesis = this.thesis();
    if (!thesis) return;
    this.editTitle.set(thesis.title);
    this.editShortDescription.set(thesis.shortDescription ?? '');
    this.editDescription.set(thesis.description ?? '');
    this.isEditing.set(true);
    this.showSuccessModal.set(false);
  }

  cancelEdit(): void {
    this.isEditing.set(false);
  }

  saveEdit(): void {
    const thesis = this.thesis();
    if (!thesis) return;
    this.isSaving.set(true);

    this.thesisService
      .updateThesis(thesis.id, {
        title: this.editTitle(),
        shortDescription: this.editShortDescription(),
        description: this.editDescription(),
        thesisUrl: thesis.thesisUrl,
        tags: thesis.tags,
      })
      .subscribe({
        next: (updated) => {
          this.thesis.set(updated);
          this.isEditing.set(false);
          this.isSaving.set(false);
          this.showSuccessModal.set(true);
        },
        error: () => {
          this.errorMessage.set('THESISSITE.ERRORS.SAVE_THESIS');
          this.isSaving.set(false);
        },
      });
  }

  closeSuccessModal(): void {
    this.showSuccessModal.set(false);
  }

  addTag(name: string): void {
    const thesis = this.thesis();
    const cleaned = name.trim();
    if (!thesis || !cleaned || this.isSavingTags()) return;

    if (thesis.tags.some((tag) => tag.toLowerCase() === cleaned.toLowerCase())) {
      this.tagsErrorMessage.set(this.translateService.instant('THESISSITE.TAGS.ERROR_DUPLICATE'));
      return;
    }

    this.saveTags(thesis, [...thesis.tags, cleaned]);
  }

  deleteTag(name: string): void {
    const thesis = this.thesis();
    if (!thesis || this.isSavingTags()) return;

    this.saveTags(thesis, thesis.tags.filter((tag) => tag.toLowerCase() !== name.toLowerCase()));
  }

  private saveTags(thesis: ThesisResponse, tags: string[]): void {
    this.isSavingTags.set(true);
    this.tagsErrorMessage.set(null);

    this.thesisService
      .updateThesis(thesis.id, {
        title: thesis.title,
        shortDescription: thesis.shortDescription,
        description: thesis.description,
        thesisUrl: thesis.thesisUrl,
        tags,
      })
      .subscribe({
        next: (updated) => {
          this.thesis.set(updated);
          this.isSavingTags.set(false);
        },
        error: () => {
          this.tagsErrorMessage.set(this.translateService.instant('THESISSITE.TAGS.ERROR_SAVE'));
          this.isSavingTags.set(false);
        },
      });
  }
}
