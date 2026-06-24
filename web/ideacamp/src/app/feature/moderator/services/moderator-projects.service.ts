import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../enviroments/enviroment.dev';
import { ProjectResponse } from '../../../models/project.model';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ProjectSearchParams {
  page: number;
  size: number;
  name?: string;
}

@Injectable({ providedIn: 'root' })
export class ModeratorProjectsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/projects`;
  private readonly v1BaseUrl = `${environment.apiUrl}/v1/projects`;

  getAllProjects(params: ProjectSearchParams): Observable<PageResponse<ProjectResponse>> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString());

    if (params.name) {
      httpParams = httpParams.set('name', params.name);
    }

    return this.http.get<PageResponse<ProjectResponse>>(this.baseUrl, { params: httpParams }).pipe(
      catchError(() => this.http.get<PageResponse<ProjectResponse>>(this.v1BaseUrl, { params: httpParams })),
    );
  }

  deleteProject(projectId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}`).pipe(
      catchError(() => this.http.delete<void>(`${this.v1BaseUrl}/${projectId}`)),
    );
  }
}
