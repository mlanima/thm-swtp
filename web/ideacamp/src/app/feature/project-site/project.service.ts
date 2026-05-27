import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/enviroment.dev';
import {ProjectResponse} from '../../models/project.model';
import { ProjectCreateData } from '../project-create/schemas/project-create.schema';

export interface UpdateProjectRequest {
  name: string;
  description: string;
}

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/projects`;

  createProject(data: ProjectCreateData & { memberIds: string [] }): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(this.baseUrl, data);
  }

  getProject(projectId: string): Observable<ProjectResponse> {
    return this.http.get<ProjectResponse>(`${this.baseUrl}/${projectId}`);
  }

  updateProject(projectId: string, request: UpdateProjectRequest): Observable<ProjectResponse> {
    return this.http.put<ProjectResponse>(`${this.baseUrl}/${projectId}`, request);
  }
}
