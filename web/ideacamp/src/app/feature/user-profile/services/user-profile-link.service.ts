import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { z } from 'zod';
import { environment } from '../../../enviroments/enviroment.dev';
import { UserProfileLinkModel, UserProfileLinkSchema, } from '../../../models/user-profile-link.model';
import { CreateLinkRequest, UpdateLinkRequest, } from '../../../shared/link-manager/link-manager.types';

@Injectable({ providedIn: 'root' })
export class UserProfileLinkService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/users`;

  getUserProfileLinks(userId: string): Observable<UserProfileLinkModel[]> {
    return this.http
      .get<unknown>(`${this.baseUrl}/${userId}/links`)
      .pipe(map((data) => z.array(UserProfileLinkSchema).parse(data)));
  }

  addUserProfileLink(userId: string, request: CreateLinkRequest): Observable<UserProfileLinkModel> {
    return this.http
      .post<unknown>(`${this.baseUrl}/${userId}/links`, request)
      .pipe(map((data) => UserProfileLinkSchema.parse(data)));
  }

  updateUserProfileLink(userId: string, linkId: string, request: UpdateLinkRequest): Observable<UserProfileLinkModel> {
    return this.http
      .patch<unknown>(`${this.baseUrl}/${userId}/links/${linkId}`, request)
      .pipe(map((data) => UserProfileLinkSchema.parse(data)));
  }

  deleteUserProfileLink(userId: string, linkId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${userId}/links/${linkId}`);
  }
}
