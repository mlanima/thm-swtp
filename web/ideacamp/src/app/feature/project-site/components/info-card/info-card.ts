import { Component, Input } from '@angular/core';
import { ProjectResponse } from '../../../../models/project.model';

@Component({
  selector: 'app-info-card',
  standalone: true,
  templateUrl: './info-card.html'
})
export class InfoCard {
  @Input({ required: true }) project!: ProjectResponse;
}
