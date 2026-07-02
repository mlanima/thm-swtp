import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../enviroments/enviroment.dev';
import { ManagedUser, PageResponse, UserStatus } from '../models/managed-user.model';

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/users/management`;


  getUsers(status: UserStatus, page = 0, size = 20): Observable<PageResponse<ManagedUser>> {
    const params = new HttpParams()
      .set('status', status)
      .set('page', page)
      .set('size', size);

    return this.http.get<PageResponse<ManagedUser>>(this.baseUrl, {params})
  }

  banUser(userId: string, reason?: string): Observable<ManagedUser> {
    return this.http.patch<ManagedUser>(`${this.baseUrl}/${userId}/ban`, {
      reason: reason || null,
    });
  }

  unbanUser(userId: string): Observable<ManagedUser> {
    return this.http.patch<ManagedUser>(`${this.baseUrl}/${userId}/unban`, {})
  }
}
