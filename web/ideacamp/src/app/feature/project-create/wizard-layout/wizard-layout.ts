import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
@Component({
  selector: 'app-wizard-layout',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './wizard-layout.html',
})
export class WizardLayout {}
