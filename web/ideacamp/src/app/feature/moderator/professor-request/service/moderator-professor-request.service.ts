import { HttpClient, HttpParams } from '@angular/common/http'
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../enviroments/enviroment.dev';
import { PageResponse } from '../../../../models/page-response.model';
import { ModeratorProfRequest } from '../models/moderator-professor-request.model';

@Injectable({ providedIn: 'root' })
export class ModeratorProfessorRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/professor-requests`;

  getRequests(page = 0, size = 20): Observable<PageResponse<ModeratorProfRequest>> {
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http.get<PageResponse<ModeratorProfRequest>>(this.baseUrl, { params });
  }

  acceptRequest(requestId: string): Observable<ModeratorProfRequest> {
    return this.http.patch<ModeratorProfRequest>(`${this.baseUrl}/${requestId}/accept`, {});
  }

  rejectRequest(requestId: string): Observable<ModeratorProfRequest> {
    return this.http.patch<ModeratorProfRequest>(`${this.baseUrl}/${requestId}/reject`, {});
  }
}
