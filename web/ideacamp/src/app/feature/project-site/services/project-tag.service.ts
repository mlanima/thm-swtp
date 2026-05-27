import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';

export interface TagResponse {
  name: string;
}

export interface CreateTagRequest {
  name: string;
}

@Injectable({ providedIn: 'root' })
export class ProjectTagService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/projects`;

  getProjectTags(projectId: string): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(`${this.baseUrl}/${projectId}/tags`);
  }

  addTag(projectId: string, request: CreateTagRequest): Observable<TagResponse> {
    return this.http.post<TagResponse>(`${this.baseUrl}/${projectId}/tags`, request);
  }
}
