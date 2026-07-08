import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';
import {
  GithubAuthorizeUrlSchema,
  GithubConnectionStatusModel,
  GithubConnectionStatusSchema,
} from '../../../models/github-connection.model';

@Injectable({ providedIn: 'root' })
export class GithubConnectionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/github/connection`;

  getStatus(): Observable<GithubConnectionStatusModel> {
    return this.http
      .get<unknown>(this.baseUrl)
      .pipe(map((data) => GithubConnectionStatusSchema.parse(data)));
  }

  getAuthorizeUrl(): Observable<string> {
    return this.http
      .post<unknown>(`${this.baseUrl}/authorize-url`, {})
      .pipe(map((data) => GithubAuthorizeUrlSchema.parse(data).authorizeUrl));
  }

  completeCallback(code: string, state: string): Observable<GithubConnectionStatusModel> {
    return this.http
      .post<unknown>(`${this.baseUrl}/callback`, { code, state })
      .pipe(map((data) => GithubConnectionStatusSchema.parse(data)));
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>(this.baseUrl);
  }
}
