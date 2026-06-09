import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';
import { ProjectMemberResponse } from '../models/project-settings.model'

@Injectable({ providedIn: 'root' })
export class ProjectSettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/projects`;

  deleteProject(projectId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}`);
  }

  getProjectMembers(projectId: string): Observable<ProjectMemberResponse[]> {
    return this.http.get<ProjectMemberResponse[]>(`${this.baseUrl}/${projectId}/members`);
  }

  deleteProjectMember(projectId: string, memberId:string): Observable<void>{
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/members/${memberId}`);
  }

}
