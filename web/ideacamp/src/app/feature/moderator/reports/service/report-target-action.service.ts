import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../enviroments/enviroment.dev';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReportTargetActionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  banUser(userId: string, reason?: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/v1/users/management/${userId}/ban`, {
      reason: reason ?? null,
    });
  }

  deleteProject(projectId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/v1/projects/${projectId}`);
  }

  deleteProjectPost(projectId: string, postId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/v1/projects/${projectId}/posts/${postId}`);
  }
}
