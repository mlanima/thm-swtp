import { Component, Input, OnChanges, SimpleChanges, inject, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProjectLinkModel} from '../../../../models/project-link.model';
import { ProjectLinkService } from '../../services/project-link.service';
import { createProjectLinkSchema, updateProjectLinkSchema, CreateProjectLinkRequest, UpdateProjectLinkRequest} from '../../schemas/project-link.schema';

@Component({
  selector: 'app-quicklinks',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './quicklinks.html',
})
export class Quicklinks implements OnChanges {
  private readonly projectLinkService = inject(ProjectLinkService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly translateService = inject(TranslateService);

  @Input({ required: true }) projectId!: string;
  @Input() isOwner = false;

  links: ProjectLinkModel[] = [];

  isLoading = false;
  errorMessage: string | null = null;
  isAdding = false;
  editingLinkId: string | null = null;

  newLabel = '';
  newUrl = '';

  updateLabel = '';
  updateUrl = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['projectId'] && this.projectId) {
      this.loadLinks();
    }
  }

  startAdd(): void {
    this.isAdding = true;
    this.editingLinkId = null;
    this.errorMessage = null;
    this.newLabel = '';
    this.newUrl = '';
  }

  cancelAdd(): void {
    this.isAdding = false;
    this.errorMessage = null;
    this.newLabel = '';
    this.newUrl = '';
  }

  startEdit(link: ProjectLinkModel): void {
    this.editingLinkId = link.id;
    this.isLoading = false;
    this.errorMessage = null;
    this.updateLabel = link.label;
    this.updateUrl = link.url;
  }

  cancelEdit(): void {
    this.editingLinkId = null;
    this.errorMessage = null;
    this.updateLabel = '';
    this.updateUrl = '';
  }

  loadLinks(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.changeDetectorRef.markForCheck();

    this.projectLinkService.getProjectLinks(this.projectId).subscribe({
      next: (links) => {
        this.links = links;
        this.isLoading = false;
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = this.translateService.instant('PROJECTSITE.QUICKLINKS.ERROR_LOAD');
        this.isLoading = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  addLink(): void {
    const request = this.validateCreateLinkRequest();
    if (!request) {
      return;
    }

    this.projectLinkService.addProjectLink(this.projectId, request).subscribe({
      next: (link) => {
        this.addCreatedProjectLink(link);
        this.cancelAdd();
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = this.translateService.instant('PROJECTSITE.QUICKLINKS.ERROR_ADD');
      },
    });
  }

  updateLink(linkId: string): void {
    const request = this.validateUpdateLinkRequest();

    if (!request) {
      return;
    }
    this.projectLinkService.updateProjectLink(this.projectId, linkId, request).subscribe({
      next: (updatedLink) => {
        this.replaceUpdatedLink(updatedLink);
        this.cancelEdit();
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = this.translateService.instant('PROJECTSITE.QUICKLINKS.ERROR_UPDATE');
      },
    });
  }

  deleteLink(linkId: string): void {
    this.projectLinkService.deleteProjectLink(this.projectId, linkId).subscribe({
      next: () => {
        this.removeDeletedLink(linkId);
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = this.translateService.instant('PROJECTSITE.QUICKLINKS.ERROR_DELETE');
      },
    });
  }

  openLink(url : string): void {
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  removeHttpFromLink(url: string): string {
    return url
      .replace(/^https?:\/\//, '')
      .replace(/\/$/, '');
  }

  private validateCreateLinkRequest(): CreateProjectLinkRequest | null {
    const res = createProjectLinkSchema.safeParse({
      label: this.newLabel,
      url: this.addsHttpsToUrl(this.newUrl),
    });

    if (!res.success) {
      this.setValidationError(res.error.issues[0]?.message);
      return null;
    }

    return res.data;
  }

  private validateUpdateLinkRequest(): UpdateProjectLinkRequest | null {
    const res = updateProjectLinkSchema.safeParse({
      label: this.updateLabel,
      url: this.addsHttpsToUrl(this.updateUrl),
    });

    if (!res.success) {
      this.setValidationError(res.error.issues[0]?.message);
      return null;
    }

    return res.data;
  }

  private setValidationError(message?: string): void {
    this.errorMessage = this.translateService.instant(message ?? 'PROJECTSITE.QUICKLINKS.ERROR_INVALID');
  }

  private addCreatedProjectLink(link: ProjectLinkModel) {
    this.links = [...this.links, link];
  }

  private replaceUpdatedLink(updatedLink: ProjectLinkModel): void {
    this.links = this.links.map((link) => (link.id === updatedLink.id ? updatedLink : link));
  }

  private removeDeletedLink(linkId: string): void {
    this.links = this.links.filter((link) => link.id !== linkId);
  }

  private addsHttpsToUrl(url : string): string {
    const trimmedUrl = url.trim();

    if (/^https?:\/\//i.test(trimmedUrl)){
      return trimmedUrl;
    }
    return `https://${trimmedUrl}`;
  }
}
