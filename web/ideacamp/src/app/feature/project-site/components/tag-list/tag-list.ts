import { Component, Input, OnChanges, OnInit, SimpleChanges, inject, signal } from '@angular/core';
import { ProjectTagService, TagResponse } from '../../services/project-tag.service';
import { EditableTagListComponent } from '../../../../shared/tags/tag-list/editable-tag-list.component';

@Component({
  selector: 'app-tag-list',
  standalone: true,
  imports: [EditableTagListComponent],
  templateUrl: './tag-list.html',
})
export class TagList implements OnInit, OnChanges {
  private readonly projectTagService = inject(ProjectTagService);

  @Input({ required: true }) projectId?: string;
  @Input() isOwner = false;


  tags = signal<TagResponse[]>([]);
  isLoading = signal(false);
  isSaving = signal(false);
  isDeleting = signal(false);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadTags();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['projectId'] && !changes['projectId'].firstChange) {
      this.loadTags();
    }
  }



  saveTag(name: string): void {
    const projectId = this.projectId;
    const cleanedName = name.trim();

    if (!projectId || !cleanedName) {
      return;
    }


    this.isSaving.set(true);
    this.errorMessage.set(null);

    this.projectTagService.addTag(projectId, { name: cleanedName }).subscribe({
      next: (tag) => {
        const lower = tag.name.toLowerCase();
        const existing = this.tags().some((item) => item.name.toLowerCase() === lower);

        if (existing) {
          this.errorMessage.set('Tag already exists on this project.');
        }

        if (!existing) {
          this.tags.set([...this.tags(), tag]);
        }
        this.isSaving.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not save tag.');
        this.isSaving.set(false);
      },
    });
  }

  deleteTag(tagName: string): void {
    const projectId = this.projectId;
    if (!projectId || !this.isOwner) return;

    this.isDeleting.set(true);
    this.errorMessage.set(null);

    this.projectTagService.deleteTag(projectId, tagName).subscribe({
      next: () => {
        const lower = tagName.toLowerCase();
        this.tags.set(this.tags().filter((tag) => tag.name.toLowerCase() !== lower));
        this.isDeleting.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not delete tag.');
        this.isDeleting.set(false);
      },
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
        this.errorMessage.set('Could not load tags.');
        this.isLoading.set(false);
      },
    });
  }
}
