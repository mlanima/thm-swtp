import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-impressum-tab',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './impressum-tab.html',
})
export class ImpressumTab {
  readonly currentYear = new Date().getFullYear();
}
