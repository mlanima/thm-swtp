import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';
import { ProjectInviteResponse } from '../../../models/project-invite.model';

@Injectable({ providedIn: 'root' })
export class ProjectInvitationService {
  private readonly http = inject(HttpClient);

  getInvitations(): Observable<ProjectInviteResponse[]> {
    return this.http.get<ProjectInviteResponse[]>(
      `${environment.apiUrl}/v1/users/me/invitations`,
    );
  }

  updateInvitationStatus(
    invitationId: string,
    status: 'ACCEPTED' | 'REJECTED',
  ): Observable<ProjectInviteResponse> {
    return this.http.patch<ProjectInviteResponse>(
      `${environment.apiUrl}/v1/invitations/${invitationId}`,
      { status },
    );
  }
}
