import { Component, Input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ThesisResponse } from '../../../../models/thesis.model';

@Component({
  selector: 'app-info-card',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './info-card.html'
})
export class InfoCard {
  @Input({ required: true }) thesis!: ThesisResponse;
}
