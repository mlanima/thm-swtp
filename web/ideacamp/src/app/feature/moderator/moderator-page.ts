import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-moderator-page',
  standalone: true,
  imports: [RouterLink, TranslatePipe],
  templateUrl: './moderator-page.html',
})
export class ModeratorPage {}
