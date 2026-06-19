import { Component, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { ProjectFileModel } from '../../../../models/project-file.model';
import { ProjectFileService } from '../../services/project-file.service';

@Component({
  selector: 'app-project-files',
  standalone: true,
  imports: [],
  templateUrl: './project-files.html',
})
export class ProjectFiles implements OnChanges {
  private readonly projectFileService = inject(ProjectFileService);

  private readonly ALLOWED_TYPES = [
    'application/pdf',
    'image/png',
    'image/jpeg',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain',
    'text/markdown',
  ];

  @Input({ required: true }) projectId!: string;
  @Input() isOwner = false;

  files = signal<ProjectFileModel[]>([]);
  isLoading = signal(false);
  isUploading = signal(false);
  errorMessage = signal<string | null>(null);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['projectId'] && this.projectId) {
      this.loadFiles();
    }
  }

  loadFiles(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectFileService.getProjectFiles(this.projectId).subscribe({
      next: (files) => {
        this.files.set(files);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Dateien konnten nicht geladen werden.');
        this.isLoading.set(false);
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.upload(file);
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (!file) return;
    this.upload(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  deleteFile(fileId: string): void {
    this.projectFileService.deleteFile(this.projectId, fileId).subscribe({
      next: () => {
        this.files.set(this.files().filter((f) => f.id !== fileId));
      },
      error: () => {
        this.errorMessage.set('Datei konnte nicht gelöscht werden.');
      },
    });
  }

  downloadUrl(fileId: string): string {
    return this.projectFileService.downloadUrl(this.projectId, fileId);
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('de-DE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  private upload(file: File): void {
    if (!this.ALLOWED_TYPES.includes(file.type)) {
      this.errorMessage.set('Dateityp nicht erlaubt. Erlaubt: PDF, PNG, JPG, DOCX, TXT, MD');
      return;
    }

    this.isUploading.set(true);
    this.errorMessage.set(null);

    this.projectFileService.uploadFile(this.projectId, file).subscribe({
      next: (uploaded) => {
        this.files.set([...this.files(), uploaded]);
        this.isUploading.set(false);
      },
      error: () => {
        this.errorMessage.set('Datei konnte nicht hochgeladen werden.');
        this.isUploading.set(false);
      },
    });
  }
}