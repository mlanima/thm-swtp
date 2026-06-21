import { Component, input, output, signal, ElementRef, ViewChild } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';


export interface TagModel {
  name: string;
}


@Component({
  selector: 'app-editable-tag-list',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './editable-tag-list.component.html',
})
export class EditableTagListComponent {
  title = input('Tags');
  tags = input<TagModel[]>([]);
  isOwner = input(false);
  isLoading = input(false);
  isSaving = input(false);
  isDeleting = input(false);
  errorMessage = input<string | null>(null);

  addTag = output<string>();
  deleteTag = output<string>();

  @ViewChild('tagInput') tagInput?: ElementRef<HTMLInputElement>;

  isAdding = signal(false);
  newTagName = signal('');


  startAdd(): void {
    if (!this.isOwner() || this.isAdding()) {
      return;
    }
    this.newTagName.set('');
    this.isAdding.set(true);

    setTimeout(() => this.tagInput?.nativeElement.focus());
  }

  cancelAdd(): void {
    this.isAdding.set(false);
    this.newTagName.set('');
  }

  saveTag(): void {
    if (this.isSaving()){
      return;
    }

    const tagName = this.newTagName().trim();

    if (!tagName) {
      this.cancelAdd();
      return;
    }

    this.addTag.emit(tagName);
    this.isAdding.set(false);
    this.newTagName.set('');
  }
}
