import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { ThesisResponse } from '../../../../models/thesis.model';
import { ThesisCard } from '../thesis-card/thesis-card';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-thesis-list',
  standalone: true,
  imports: [ThesisCard, TranslatePipe],
  templateUrl: './thesis-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThesisList {
  readonly theses = input.required<ThesisResponse[]>();
  readonly isLoading = input.required<boolean>();
  readonly errorMessage = input.required<string>();
}