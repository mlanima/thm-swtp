import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfileModel } from '../models/user-profile.model';
import { environment } from '../enviroments/enviroment.dev';

/**
 * Provides HTTP access to user profile endpoints
 *
 * The service contains methods for loading the authenticated user's own profile
 * and for loading public profile data by username
 */
@Injectable({
  providedIn: 'root',
})
export class UserProfileService {
  /**
   * Base URL for all user-related backend endpoints
   *
   * Built from the configured API URL and the '/users' resource path
   */
  private readonly apiUrl = `${environment.apiUrl}/users`;


  /** Angular HTTP client used to communicate with the backend */
  private readonly http = inject(HttpClient);

  /**
   * Loads the profile of the currently authenticated user
   *
   * The backend identifies the user through the authentication token attached to the request
   *
   * @returns An observable that emits the current user's profile
   */
  getMyProfile(): Observable<UserProfileModel> {
    return this.http.post<UserProfileModel>(`${this.apiUrl}/me`, {});
  }

  /**
   * Loads a public profile by username
   *
   * @param username Username of the profile that should be loaded
   * @returns An observable that emits the matching user profile
   */
  getProfile(username: string): Observable<UserProfileModel> {
    return this.http.get<UserProfileModel>(`${this.apiUrl}/${username}/profile`);
  }
}
