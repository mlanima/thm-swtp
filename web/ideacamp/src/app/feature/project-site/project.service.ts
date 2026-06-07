import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/enviroment.dev';
import {ProjectResponse} from '../../models/project.model';

export interface CreateProjectRequest {
  name: string;
  description?: string | null;
  projectUrl: string;
  isPrivateProject: boolean;
  memberIds: string[];
  tagIds: string[];
}

export interface UpdateProjectRequest {
  name: string;
  description: string;
  projectUrl: string;
  isPrivateProject: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/projects`;

  createProject(data: CreateProjectRequest): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(this.baseUrl, data);
  }

  getProject(projectId: string): Observable<ProjectResponse> {
    return this.http.get<ProjectResponse>(`${this.baseUrl}/${projectId}`);
  }

  getProjectByUrl(projectUrl: string): Observable<ProjectResponse> {
    return this.http.get<ProjectResponse>(`${this.baseUrl}/by-url/${projectUrl}`);
  }

  updateProject(projectId: string, request: UpdateProjectRequest): Observable<ProjectResponse> {
    return this.http.put<ProjectResponse>(`${this.baseUrl}/${projectId}`, request);
  }

  updateAllowJoinRequests(projectId: string, allow: boolean): Observable<ProjectResponse> {
    return this.http.patch<ProjectResponse>(`${this.baseUrl}/${projectId}/allow-join-requests`, null, {
      params: { allow: String(allow) },
    });
  }
}
