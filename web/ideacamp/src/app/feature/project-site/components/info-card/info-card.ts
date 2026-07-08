import { Component, Input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectResponse } from '../../../../models/project.model';
import { MarkdownPipe } from '../../../../shared/pipes/markdown.pipe';

@Component({
  selector: 'app-info-card',
  standalone: true,
  imports: [TranslatePipe, MarkdownPipe],
  templateUrl: './info-card.html'
})
export class InfoCard {
  @Input({ required: true }) project!: ProjectResponse;
}
