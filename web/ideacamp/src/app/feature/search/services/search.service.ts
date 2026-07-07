import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { z } from 'zod';
import { environment } from '../../../enviroments/enviroment.dev';
import {
  ProjectSearchResult,
  ProjectSearchResultSchema,
} from '../models/project-search-result.model';
import { UserSearchResult, UserSearchResultSchema } from '../models/user-search-result.model';
import { Page, PageSchema } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly http = inject(HttpClient);

  searchUsers(queries: string[]): Observable<UserSearchResult[]> {
    return this.http
      .get<unknown>(`${environment.apiUrl}/v1/search/users`, { params: { q: queries } })
      .pipe(map((data) => z.array(UserSearchResultSchema).parse(data)));
  }

  searchProjectsPaged(queries: string[], page: number, size = 20): Observable<Page<ProjectSearchResult>> {
    return this.http
      .get<unknown>(`${environment.apiUrl}/v1/search/projects/paged`, {
        params: { q: queries, page: page.toString(), size: size.toString() },
      })
      .pipe(map((data) => PageSchema(ProjectSearchResultSchema).parse(data)));
  }

  searchUsersPaged(queries: string[], page: number, size = 20): Observable<Page<UserSearchResult>> {
    return this.http
      .get<unknown>(`${environment.apiUrl}/v1/search/users/paged`, {
        params: { q: queries, page: page.toString(), size: size.toString() },
      })
      .pipe(map((data) => PageSchema(UserSearchResultSchema).parse(data)));
  }

  searchTags(query: string, limit = 32): Observable<string[]> {
    return this.http
      .get<
        { name: string }[]
      >(`${environment.apiUrl}/v1/tags`, { params: { q: query, limit: limit.toString() } })
      .pipe(map((tags) => tags.map((t) => t.name)));
  }

  getProjectTags(projectId: string): Observable<string[]> {
    return this.http
      .get<{ name: string }[]>(`${environment.apiUrl}/v1/projects/${projectId}/tags`)
      .pipe(map((tags) => tags.map((t) => t.name)));
  }
}
