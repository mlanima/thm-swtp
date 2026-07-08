import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfileModel } from '../models/user-profile.model';
import { environment } from '../enviroments/enviroment.dev';

export interface UpdateUserProfileRequest {
  title?: string | null;
  location?: string | null;
  about?: string | null;
  experience?: string | null;
  placeId?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class UserProfileService {
  private readonly apiUrl = `${environment.apiUrl}/v1/users`;

  private readonly http = inject(HttpClient);

  getMyProfile(): Observable<UserProfileModel> {
    return this.http.post<UserProfileModel>(`${this.apiUrl}/me`, {});
  }

  getProfile(username: string): Observable<UserProfileModel> {
    return this.http.get<UserProfileModel>(`${this.apiUrl}/${username}/profile`);
  }

  updateProfile(username: string, profile: UpdateUserProfileRequest): Observable<UserProfileModel> {
    return this.http.put<UserProfileModel>(`${this.apiUrl}/${username}/profile`, profile);
  }
}
