import { Component, ElementRef, Input, OnChanges, OnInit, SimpleChanges, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectTagService, TagResponse } from '../../services/project-tag.service';

@Component({
  selector: 'app-tag-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tag-list.html'
})
export class TagList implements OnInit, OnChanges {
  private readonly projectTagService = inject(ProjectTagService);

  @Input({ required: true }) projectId?: string;
  @Input() isOwner = false;

  @ViewChild('tagInput') tagInput?: ElementRef<HTMLInputElement>;

  tags = signal<TagResponse[]>([]);
  isLoading = signal(false);
  isAdding = signal(false);
  isSaving = signal(false);
  errorMessage = signal<string | null>(null);
  newTagName = signal('');

  ngOnInit(): void {
    this.loadTags();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['projectId'] && !changes['projectId'].firstChange) {
      this.loadTags();
    }
  }

  startAdd(): void {
    if (!this.isOwner || this.isAdding()) return;
    this.errorMessage.set(null);
    this.newTagName.set('');
    this.isAdding.set(true);

    setTimeout(() => this.tagInput?.nativeElement.focus());
  }

  cancelAdd(): void {
    this.isAdding.set(false);
    this.newTagName.set('');
  }

  saveTag(): void {
    const projectId = this.projectId;
    const name = this.newTagName().trim();

    if (!projectId) return;
    if (!name) {
      this.cancelAdd();
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);

    this.projectTagService.addTag(projectId, { name }).subscribe({
      next: (tag) => {
        const lower = tag.name.toLowerCase();
        const existing = this.tags().some((item) => item.name.toLowerCase() === lower);
        if (!existing) {
          this.tags.set([...this.tags(), tag]);
        }
        this.isSaving.set(false);
        this.isAdding.set(false);
        this.newTagName.set('');
      },
      error: () => {
        this.errorMessage.set('Tag konnte nicht gespeichert werden.');
        this.isSaving.set(false);
      }
    });
  }

  private loadTags(): void {
    if (!this.projectId) return;
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectTagService.getProjectTags(this.projectId).subscribe({
      next: (tags) => {
        this.tags.set(tags);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Tags konnten nicht geladen werden.');
        this.isLoading.set(false);
      }
    });
  }
}
