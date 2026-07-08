import { Component, Input, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink } from '@angular/router';
import { ThesisResponse } from '../../../../models/thesis.model';
import { AuthService } from '../../../auth/auth.service';

@Component({
  selector: 'app-thesis-header',
  standalone: true,
  imports: [RouterLink, TranslatePipe],
  templateUrl: './thesis-header.html',
})
export class ThesisHeader {
  @Input({ required: true }) thesis!: ThesisResponse;

  private readonly authService = inject(AuthService);

  get isSupervisor(): boolean {
    const user = this.authService.user();
    if (!user || !this.thesis) return false;
    return user.id === this.thesis.supervisorKeycloakId;
  }
}
