import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';

export interface ProfessorRequestResponse {
  id: string;
  requestingUserId: string;
  requestingUsername: string;
  email: string;
  text: string;
  createdAt: string;
  updatedAt: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
}

export interface CreateProfessorRequest {
  email: string;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ProfessorRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/professor-requests`;

  getMyRequest(userId: string): Observable<ProfessorRequestResponse | null> {
    return this.http.get<ProfessorRequestResponse[]>(`${this.baseUrl}/${userId}`).pipe(
      map((requests) => {
        if (requests.length === 0) return null;
        return requests.sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
        )[0];
      }),
    );
  }

  create(data: CreateProfessorRequest): Observable<ProfessorRequestResponse> {
    return this.http.post<ProfessorRequestResponse>(this.baseUrl, data);
  }
}
