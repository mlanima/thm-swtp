import { Component, Input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
@Component({
  selector: 'app-wizard-layout',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './wizard-layout.html',
})
/** Generic layout for multi-step creation wizards (e.g. project or thesis creation). */
export class WizardLayout {
  @Input() titleKey = 'PROJECTCREATE.PAGE_TITLE';
}
