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
export class UserProfileTagService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/users`;

  getProfileTags(userid: string): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(`${this.baseUrl}/${userid}/profile/tags`);
  }

  addTag(userId: string, request: CreateTagRequest): Observable<TagResponse> {
    return this.http.post<TagResponse>(`${this.baseUrl}/me/profile/tags`, request);
  }

  deleteTag(tagName: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/me/profile/tags/${encodeURIComponent(tagName)}`,
    );
  }
}
