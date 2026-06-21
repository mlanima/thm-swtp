import { Component, inject } from '@angular/core';
import { AuthService } from '../../../auth/auth.service';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './landing-page.html',
})
export class LandingPage {
  auth = inject(AuthService);
}
