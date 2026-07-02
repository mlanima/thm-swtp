import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../enviroments/enviroment.dev';
import { ManagedUser, UserStatus, ManagedUserSortField, SortDirection } from '../models/managed-user.model';
import { PageResponse } from '../../../../models/page-response.model'

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/users/management`;


  getUsers(status: UserStatus, page = 0, size = 10, sortField: ManagedUserSortField = 'username',
           sortDirection: SortDirection = 'asc'): Observable<PageResponse<ManagedUser>> {
    const params = new HttpParams()
      .set('status', status)
      .set('page', page)
      .set('size', size)
      .set('sort', `${sortField},${sortDirection}`);

    return this.http.get<PageResponse<ManagedUser>>(this.baseUrl, {params});
  }

  banUser(userId: string, reason?: string): Observable<ManagedUser> {
    return this.http.patch<ManagedUser>(`${this.baseUrl}/${userId}/ban`, {
      reason: reason || null,
    });
  }

  unbanUser(userId: string): Observable<ManagedUser> {
    return this.http.patch<ManagedUser>(`${this.baseUrl}/${userId}/unban`, {});
  }
}
