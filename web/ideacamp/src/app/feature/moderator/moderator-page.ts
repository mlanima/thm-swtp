import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-moderator-page',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './moderator-page.html',
})
export class ModeratorPage {}
