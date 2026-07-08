import {Component, input, output} from '@angular/core';
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
  readonly projectData = input<Partial<ProjectCreateData>>({});
  readonly members = input<ProjectInviteMember[]>([]);
  readonly isLoading = input(false);

  readonly back = output<void>();
  readonly finished = output<void>();
}
