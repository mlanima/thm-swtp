import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/enviroment.dev';
import { ThesisResponse } from '../../models/thesis.model';

export interface CreateThesisRequest {
  title: string;
  shortDescription?: string | null;
  description?: string | null;
  thesisUrl: string;
  tags: string[];
}

export interface UpdateThesisRequest {
  title: string;
  shortDescription?: string;
  description: string;
  thesisUrl: string;
  tags: string[];
}

@Injectable({ providedIn: 'root' })
export class ThesisService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/theses`;

  createThesis(data: CreateThesisRequest): Observable<ThesisResponse> {
    return this.http.post<ThesisResponse>(this.baseUrl, data);
  }

  getThesis(thesisId: string): Observable<ThesisResponse> {
    return this.http.get<ThesisResponse>(`${this.baseUrl}/${thesisId}`);
  }

  getThesisByUrl(thesisUrl: string): Observable<ThesisResponse> {
    return this.http.get<ThesisResponse>(`${this.baseUrl}/by-url/${thesisUrl}`);
  }

  updateThesis(thesisId: string, request: UpdateThesisRequest): Observable<ThesisResponse> {
    return this.http.put<ThesisResponse>(`${this.baseUrl}/${thesisId}`, request);
  }

  thesisUrlExists(thesisUrl: string) {
    return this.http.get<boolean>(`${this.baseUrl}/url-exists/${encodeURIComponent(thesisUrl)}`);
  }
}
