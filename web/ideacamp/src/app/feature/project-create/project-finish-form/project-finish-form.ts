import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ProjectCreateData} from '../schemas/project-create.schema';
import { ProjectInviteMember } from '../../../models/project-invite-member.model';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-project-finish-form',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './project-finish-form.html',
})

/** Last step of the project creation wizard.
 * Shows summary of collected project data
 * */
export class ProjectFinishForm {
  @Input() projectData : Partial<ProjectCreateData> = {};
  @Input() members : ProjectInviteMember[] = [];

  @Output() back = new EventEmitter<void>();
  @Output() finished = new EventEmitter<void>();
}
