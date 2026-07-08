import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ThesisCreateData} from '../schemas/thesis-create.schema';
import { ProjectInviteMember } from '../../../models/project-invite-member.model';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-thesis-finish-form',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './thesis-finish-form.html',
})

/** Last step of the thesis creation wizard.
 * Shows summary of collected thesis data
 * */
export class ThesisFinishForm {
  @Input() thesisData : Partial<ThesisCreateData> = {};
  @Input() students : ProjectInviteMember[] = [];

  @Output() back = new EventEmitter<void>();
  @Output() finished = new EventEmitter<void>();
}
