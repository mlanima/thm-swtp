import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';
import { UnreadThesisNotificationCountResponse } from '../../../models/thesis.model';

@Injectable({ providedIn: 'root' })
export class ThesisNotificationService {
  private readonly http = inject(HttpClient);

  getUnreadCount(): Observable<UnreadThesisNotificationCountResponse> {
    return this.http.get<UnreadThesisNotificationCountResponse>(
      `${environment.apiUrl}/v1/users/me/thesis-notifications/unread-count`,
    );
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/v1/users/me/thesis-notifications/mark-read`, {});
  }
}
