import { Component, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProjectFileModel } from '../../../../models/project-file.model';
import { FileVisibility } from '../../../../models/file-visibility.model';
import { ProjectFileService } from '../../services/project-file.service';

@Component({
  selector: 'app-project-files',
  standalone: true,
  imports: [TranslatePipe, NgClass],
  templateUrl: './project-files.html',
})
export class ProjectFiles implements OnChanges {
  private readonly projectFileService = inject(ProjectFileService);
  private readonly translateService = inject(TranslateService);

  private readonly ALLOWED_TYPES = [
    'application/pdf',
    'image/png',
    'image/jpeg',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain',
    'text/markdown',
    'text/x-markdown',
  ];

  private readonly ALLOWED_EXTENSIONS = ['pdf', 'png', 'jpg', 'jpeg', 'docx', 'txt', 'md'];

  @Input({ required: true }) projectId!: string;
  @Input() isOwner = false;

  files = signal<ProjectFileModel[]>([]);
  isLoading = signal(false);
  isUploading = signal(false);
  errorMessage = signal<string | null>(null);
  uploadVisibility = signal<FileVisibility>('PUBLIC');

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
        this.errorMessage.set(this.translateService.instant('PROJECTSITE.FILES.ERROR_LOAD'));
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

  toggleUploadVisibility(): void {
    this.uploadVisibility.set(this.uploadVisibility() === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC');
  }

  toggleFileVisibility(file: ProjectFileModel): void {
    const nextVisibility: FileVisibility = file.visibility === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC';

    this.projectFileService.updateFileVisibility(this.projectId, file.id, nextVisibility).subscribe({
      next: (updated) => {
        this.files.set(this.files().map((f) => (f.id === updated.id ? updated : f)));
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('PROJECTSITE.FILES.ERROR_UPDATE_VISIBILITY'));
      },
    });
  }

  deleteFile(fileId: string): void {
    this.projectFileService.deleteFile(this.projectId, fileId).subscribe({
      next: () => {
        this.files.set(this.files().filter((f) => f.id !== fileId));
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('PROJECTSITE.FILES.ERROR_DELETE'));
      },
    });
  }

  downloadFile(fileId: string, fileName: string): void {
    this.projectFileService.downloadFile(this.projectId, fileId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('PROJECTSITE.FILES.ERROR_DOWNLOAD'));
      },
    });
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

  private isAllowedFile(file: File): boolean {
    if (file.type) return this.ALLOWED_TYPES.includes(file.type);
    const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
    return this.ALLOWED_EXTENSIONS.includes(ext);
  }

  private readonly MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

  private upload(file: File): void {
    if (!this.isAllowedFile(file)) {
      this.errorMessage.set(this.translateService.instant('PROJECTSITE.FILES.ERROR_TYPE_NOT_ALLOWED'));
      return;
    }

    if (file.size > this.MAX_FILE_SIZE_BYTES) {
      this.errorMessage.set(this.translateService.instant('PROJECTSITE.FILES.ERROR_FILE_TOO_LARGE'));
      return;
    }

    this.isUploading.set(true);
    this.errorMessage.set(null);

    this.projectFileService.uploadFile(this.projectId, file, this.uploadVisibility()).subscribe({
      next: (uploaded) => {
        this.files.set([...this.files(), uploaded]);
        this.isUploading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('PROJECTSITE.FILES.ERROR_UPLOAD'));
        this.isUploading.set(false);
      },
    });
  }
}
