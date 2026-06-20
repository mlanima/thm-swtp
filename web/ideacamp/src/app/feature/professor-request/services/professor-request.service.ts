import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';

export interface ProfessorRequestResponse {
  id: string;
  requestingUserId: string;
  requestingUsername: string;
  name: string;
  email: string;
  text: string;
  createdAt: string;
  updatedAt: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
}

export interface CreateProfessorRequest {
  name: string;
  email: string;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ProfessorRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/professor-requests`;

  getAll(): Observable<ProfessorRequestResponse[]> {
    return this.http
      .get<{ content: ProfessorRequestResponse[] }>(this.baseUrl, {
        params: { size: '100' },
      })
      .pipe(map((page) => page.content));
  }

  getMyRequest(userId: string): Observable<ProfessorRequestResponse | null> {
    return this.getAll().pipe(
      map((requests) => {
        const mine = requests.filter((r) => r.requestingUserId === userId);
        if (mine.length === 0) return null;
        return mine.sort(
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
