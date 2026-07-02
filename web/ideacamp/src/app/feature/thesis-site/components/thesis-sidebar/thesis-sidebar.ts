import { Component, computed, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { EditableTagListComponent, TagModel } from '../../../../shared/tags/tag-list/editable-tag-list.component';
import { StudentList } from '../student-list/student-list';

@Component({
  selector: 'app-thesis-sidebar',
  standalone: true,
  imports: [EditableTagListComponent, StudentList, TranslatePipe],
  templateUrl: './thesis-sidebar.html',
})
export class ThesisSidebar {
  thesisId = input.required<string>();
  isSupervisor = input(false);
  supervisorKeycloakId = input('');
  supervisorUsername = input('');
  tags = input<string[]>([]);
  isSavingTags = input(false);
  tagsErrorMessage = input<string | null>(null);

  addTag = output<string>();
  deleteTag = output<string>();

  readonly tagModels = computed<TagModel[]>(() => this.tags().map((name) => ({ name })));
}
