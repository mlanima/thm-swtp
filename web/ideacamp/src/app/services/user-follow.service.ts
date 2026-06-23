import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../enviroments/enviroment.dev';
import { UserProfileModel } from '../models/user-profile.model';

@Injectable({ providedIn: 'root' })
export class UserFollowService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/users`;

  followUser(username: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${username}/followers`, null);
  }

  unfollowUser(username: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${username}/followers`);
  }

  isFollowing(username: string): Observable<boolean> {
    return this.http
      .get(`${this.baseUrl}/${username}/followers/me`, { observe: 'response' })
      .pipe(
        map(() => true),
        catchError(() => of(false))
      );
  }

  getFollowers(username: string): Observable<UserProfileModel[]> {
    return this.http.get<UserProfileModel[]>(`${this.baseUrl}/${username}/followers`);
  }

  getFollowing(username: string): Observable<UserProfileModel[]> {
    return this.http.get<UserProfileModel[]>(`${this.baseUrl}/${username}/following`);
  }
}
