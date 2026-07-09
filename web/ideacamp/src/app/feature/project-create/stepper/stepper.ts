import {Component, Input} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
@Component({
  selector: 'app-stepper',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './stepper.html',
})
/** Displays the steps of a creation wizard (project or thesis).*/
export class Stepper {

  @Input() currentStep = 0;

  @Input() steps: string[] = [
    'PROJECTCREATE.STEPS.GENERAL',
    'PROJECTCREATE.STEPS.SETTINGS',
    'PROJECTCREATE.STEPS.MEMBERS',
    'PROJECTCREATE.STEPS.FINISH'
  ];

}
