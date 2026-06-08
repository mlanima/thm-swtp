import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../enviroments/enviroment.dev';

export interface JoinRequestResponse {
  id: string;
  projectId: string;
  requestingUser: string;
  requestingUsername: string;
  message: string | null;
  createdAt: string;
  updatedAt: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
}

@Injectable({ providedIn: 'root' })
export class ProjectJoinRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1`;

  getMyRequests(): Observable<JoinRequestResponse[]> {
    return this.http.get<JoinRequestResponse[]>(`${this.baseUrl}/project-join-requests/me`);
  }

  hasPendingRequest(projectId: string): Observable<boolean> {
    return this.getMyRequests().pipe(
      map(requests => requests.some(r => r.projectId === projectId && r.status === 'PENDING')),
      catchError(() => of(false))
    );
  }

  sendJoinRequest(projectId: string): Observable<JoinRequestResponse> {
    return this.http.post<JoinRequestResponse>(`${this.baseUrl}/projects/${projectId}/join-requests`, {});
  }

  getProjectRequests(projectId: string): Observable<JoinRequestResponse[]> {
    return this.http.get<JoinRequestResponse[]>(`${this.baseUrl}/projects/${projectId}/join-requests`);
  }

  acceptRequest(requestId: string): Observable<JoinRequestResponse> {
    return this.http.patch<JoinRequestResponse>(`${this.baseUrl}/project-join-requests/${requestId}/accept`, {});
  }

  rejectRequest(requestId: string): Observable<JoinRequestResponse> {
    return this.http.patch<JoinRequestResponse>(`${this.baseUrl}/project-join-requests/${requestId}/reject`, {});
  }
}
