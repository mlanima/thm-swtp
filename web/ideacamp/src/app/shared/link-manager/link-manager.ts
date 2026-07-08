import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges, inject, } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LinkVisibility } from '../../models/link-visibility.model';
import { createLinkSchema, updateLinkSchema, CreateLinkRequest, UpdateLinkRequest } from './link-manager.schema';
import { LinkManagerDataSource, LinkManager } from './link-manager.types';

@Component({
  selector: 'app-link-manager',
  standalone: true,
  imports: [FormsModule, TranslatePipe, NgClass],
  templateUrl: './link-manager.html',
})
export class LinkManagerComponent implements OnChanges {
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly translateService = inject(TranslateService);

  @Input({ required: true }) dataSource!: LinkManagerDataSource;
  @Input() isOwner = false;
  @Input() allowVisibility = false;

  links: LinkManager[] = [];

  isLoading = false;
  errorMessage: string | null = null;
  isAdding = false;
  editingLinkId: string | null = null;

  newLabel = '';
  newUrl = '';
  newVisibility: LinkVisibility = 'PUBLIC';

  updateLabel = '';
  updateUrl = '';
  updateVisibility: LinkVisibility = 'PUBLIC';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['dataSource'] && this.dataSource) {
      this.loadLinks();
    }
  }

  startAdd(): void {
    this.isAdding = true;
    this.editingLinkId = null;
    this.errorMessage = null;
    this.newLabel = '';
    this.newUrl = '';
    this.newVisibility = 'PUBLIC';
  }

  cancelAdd(): void {
    this.isAdding = false;
    this.errorMessage = null;
    this.newLabel = '';
    this.newUrl = '';
    this.newVisibility = 'PUBLIC';
  }

  startEdit(link: LinkManager): void {
    this.editingLinkId = link.id;
    this.isLoading = false;
    this.errorMessage = null;
    this.updateLabel = link.label;
    this.updateUrl = link.url;
    this.updateVisibility = link.visibility ?? 'PUBLIC';
  }

  cancelEdit(): void {
    this.editingLinkId = null;
    this.errorMessage = null;
    this.updateLabel = '';
    this.updateUrl = '';
    this.updateVisibility = 'PUBLIC';
  }

  loadLinks(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.changeDetectorRef.markForCheck();

    this.dataSource.load().subscribe({
      next: (links) => {
        this.links = links;
        this.isLoading = false;
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = this.translateService.instant('COMMON.QUICKLINKS.ERROR_LOAD');
        this.isLoading = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  toggleNewVisibility(): void {
    this.newVisibility = this.newVisibility === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC';
  }

  toggleUpdateVisibility(): void {
    this.updateVisibility = this.updateVisibility === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC';
  }

  addLink(): void {
    const request = this.validateCreateLinkRequest();

    if (!request) {
      return;
    }

    this.dataSource.createLink(request).subscribe({
      next: (link) => {
        this.links = [...this.links, link];
        this.cancelAdd();
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = this.translateService.instant('COMMON.QUICKLINKS.ERROR_ADD');
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  updateLink(linkId: string): void {
    const request = this.validateUpdateLinkRequest();

    if (!request) {
      return;
    }

    this.dataSource.updateLink(linkId, request).subscribe({
      next: (updatedLink) => {
        this.links = this.links.map((link) => (link.id === updatedLink.id ? updatedLink : link));

        this.cancelEdit();
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = this.translateService.instant('COMMON.QUICKLINKS.ERROR_UPDATE');
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  deleteLink(linkId: string): void {
    this.dataSource.deleteLink(linkId).subscribe({
      next: () => {
        this.links = this.links.filter((link) => link.id !== linkId);
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.errorMessage = this.translateService.instant('COMMON.QUICKLINKS.ERROR_DELETE');
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  openLink(url: string): void {
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  removeHttpFromLink(url: string): string {
    return url.replace(/^https?:\/\//, '').replace(/\/$/, '');
  }

  private validateCreateLinkRequest(): CreateLinkRequest | null {
    const request = {
      label: this.newLabel,
      url: this.addHttpsToUrl(this.newUrl),
      ...(this.allowVisibility ? { visibility: this.newVisibility } : {}),
    };

    const res = createLinkSchema.safeParse(request);

    if (!res.success) {
      this.setValidationError(res.error.issues[0]?.message);
      return null;
    }

    return res.data;
  }

  private validateUpdateLinkRequest(): UpdateLinkRequest | null {
    const request = {
      label: this.updateLabel,
      url: this.addHttpsToUrl(this.updateUrl),
      ...(this.allowVisibility ? { visibility: this.updateVisibility } : {}),
    };

    const res = updateLinkSchema.safeParse(request);

    if (!res.success) {
      this.setValidationError(res.error.issues[0]?.message);
      return null;
    }

    return res.data;
  }

  private setValidationError(message?: string): void {
    this.errorMessage = this.translateService.instant(message ?? 'COMMON.QUICKLINKS.ERROR_INVALID');
    this.changeDetectorRef.markForCheck();
  }

  private addHttpsToUrl(url: string): string {
    const trimmedUrl = url.trim();

    if (/^https?:\/\//i.test(trimmedUrl)) {
      return trimmedUrl;
    }

    return `https://${trimmedUrl}`;
  }
}
