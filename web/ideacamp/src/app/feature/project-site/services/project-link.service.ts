import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../enviroments/enviroment.dev';
import { ProjectLinkModel, ProjectLinkSchema} from '../../../models/project-link.model'
import { Observable, map } from 'rxjs';
import {CreateProjectLinkRequest, UpdateProjectLinkRequest} from '../schemas/project-link.schema'
import { z } from 'zod';


@Injectable({ providedIn: 'root' })
export class ProjectLinkService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/projects`;


  getProjectLinks(projectId : string): Observable<ProjectLinkModel[]> {
    return this.http.get<unknown>(`${this.baseUrl}/${projectId}/links`).pipe(map(
      data => z.array(ProjectLinkSchema).parse(data)));
  }

  addProjectLink(projectId : string, request: CreateProjectLinkRequest): Observable<ProjectLinkModel> {
    return this.http.post<unknown>(`${this.baseUrl}/${projectId}/links`, request).pipe(map(
      data => ProjectLinkSchema.parse(data)));
  }

  updateProjectLink(projectId: string, linkId : string, request: UpdateProjectLinkRequest): Observable<ProjectLinkModel>{
    return this.http.patch<unknown>(`${this.baseUrl}/${projectId}/links/${linkId}`, request).pipe(map(
      data => ProjectLinkSchema.parse(data)));
  }

  deleteProjectLink(projectId: string, linkId: string): Observable<void>{
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/links/${linkId}`);
  }
}
