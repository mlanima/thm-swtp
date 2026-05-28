import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';
import { ProjectResponse } from '../../../models/project.model';

@Injectable({ providedIn: 'root' })
export class MyProjectsService {
  private readonly http = inject(HttpClient);

  getMyProjects(username: string): Observable<ProjectResponse[]> {
    return this.http.get<ProjectResponse[]>(`${environment.apiUrl}/users/${encodeURIComponent(username)}/projects`);
  }
}



