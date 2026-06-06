import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { z } from 'zod';
import { environment } from '../../../enviroments/enviroment.dev';
import { ProjectSearchResult, ProjectSearchResultSchema } from '../models/project-search-result.model';
import { UserSearchResult, UserSearchResultSchema } from '../models/user-search-result.model';

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly http = inject(HttpClient);

  searchProjects(queries: string[]): Observable<ProjectSearchResult[]> {
    return this.http
      .get<unknown>(`${environment.apiUrl}/search/projects`, { params: { q: queries } })
      .pipe(map(data => z.array(ProjectSearchResultSchema).parse(data)));
  }

  searchUsers(queries: string[]): Observable<UserSearchResult[]> {
    return this.http
      .get<unknown>(`${environment.apiUrl}/search/users`, { params: { q: queries } })
      .pipe(map(data => z.array(UserSearchResultSchema).parse(data)));
  }
}
