import { Component } from '@angular/core';

@Component({
  selector: 'app-impressum-tab',
  standalone: true,
  templateUrl: './impressum-tab.html',
})
export class ImpressumTab {
  readonly currentYear = new Date().getFullYear();
}
