import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-impressum',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './impressum.html'
})
export class Impressum {
  readonly currentYear = new Date().getFullYear();
}
