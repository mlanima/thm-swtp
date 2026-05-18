import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-stepper',
  standalone: true,
  templateUrl: './stepper.html',
})
export class Stepper {

  @Input() currentStep = 0;

  steps = [
    'General',
    'Settings',
    'Members',
    'Finish'
  ];

}
