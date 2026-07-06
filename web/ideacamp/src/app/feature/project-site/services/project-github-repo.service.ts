import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';
import { GithubRepoCardModel, GithubRepoCardSchema } from '../../../models/github-repo-card.model';
import { GithubReadmeModel, GithubReadmeSchema } from '../../../models/github-readme.model';

export interface LinkGithubRepoRequest {
  repoOwner: string;
  repoName: string;
}

@Injectable({ providedIn: 'root' })
export class ProjectGithubRepoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/projects`;

  getRepoCard(projectId: string): Observable<GithubRepoCardModel | null> {
    return this.http.get<unknown>(`${this.baseUrl}/${projectId}/github-repo`).pipe(
      map((data) => GithubRepoCardSchema.parse(data)),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          return of(null);
        }
        return throwError(() => error);
      }),
    );
  }

  linkRepo(projectId: string, request: LinkGithubRepoRequest): Observable<GithubRepoCardModel> {
    return this.http
      .put<unknown>(`${this.baseUrl}/${projectId}/github-repo`, request)
      .pipe(map((data) => GithubRepoCardSchema.parse(data)));
  }

  unlinkRepo(projectId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/github-repo`);
  }

  getReadme(projectId: string): Observable<GithubReadmeModel | null> {
    return this.http.get<unknown>(`${this.baseUrl}/${projectId}/github-repo/readme`).pipe(
      map((data) => GithubReadmeSchema.parse(data)),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          return of(null);
        }
        return throwError(() => error);
      }),
    );
  }

  setReadmeVisibility(projectId: string, show: boolean): Observable<GithubRepoCardModel> {
    return this.http
      .patch<unknown>(`${this.baseUrl}/${projectId}/github-repo/readme-visibility`, { show })
      .pipe(map((data) => GithubRepoCardSchema.parse(data)));
  }
}
