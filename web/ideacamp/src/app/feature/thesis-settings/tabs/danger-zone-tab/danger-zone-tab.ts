import { Component, signal, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ThesisSettingsService } from '../../services/thesis-settings.service';
import { ThesisSettingsStore } from '../../thesis-settings.store';

@Component({
  selector: 'app-danger-zone-tab',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './danger-zone-tab.html',
})
export class DangerZoneTab {
  private readonly store = inject(ThesisSettingsStore);
  private readonly settingsService = inject(ThesisSettingsService);
  private readonly router = inject(Router);
  private readonly translateService = inject(TranslateService);

  showDeleteModal = signal(false);
  deleteConfirmInput = signal('');
  isDeleting = signal(false);
  deleteError = signal<string | null>(null);

  thesisTitle = computed(() => this.store.thesis()?.title ?? '');
  thesisId = computed(() => this.store.thesis()?.id ?? '');

  deleteEnabled = computed(() => this.deleteConfirmInput() === this.thesisTitle());

  openDeleteModal(): void {
    this.deleteConfirmInput.set('');
    this.deleteError.set(null);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.deleteConfirmInput.set('');
    this.deleteError.set(null);
  }

  confirmDelete(): void {
    if (!this.deleteEnabled() || this.isDeleting()) return;
    this.isDeleting.set(true);
    this.settingsService.deleteThesis(this.thesisId()).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: () => {
        this.deleteError.set(this.translateService.instant('THESISSETTINGS.DANGER.ERROR_DELETE_THESIS'));
        this.isDeleting.set(false);
      },
    });
  }
}
