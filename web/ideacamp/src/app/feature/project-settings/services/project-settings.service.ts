import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';
import { ProjectMemberResponse } from '../models/project-settings.model'
import { CreateProjectInviteRequest, ProjectInviteResponse } from '../../../models/project-invite.model'

@Injectable({ providedIn: 'root' })
export class ProjectSettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/projects`;

  deleteProject(projectId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}`);
  }

  getProjectMembers(projectId: string): Observable<ProjectMemberResponse[]> {
    return this.http.get<ProjectMemberResponse[]>(`${this.baseUrl}/${projectId}/members`);
  }

  deleteProjectMember(projectId: string, memberId:string): Observable<void>{
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/members/${memberId}`);
  }

  createProjectInvite(projectId: string, request: CreateProjectInviteRequest): Observable<void>{
    return this.http.post<void>(`${this.baseUrl}/${projectId}/invitations`, request);
  }

  getProjectInvites(projectId: string){
    return this.http.get<ProjectInviteResponse[]>(`${this.baseUrl}/${projectId}/invitations`);
  }

}
