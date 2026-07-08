import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';
import { ThesisResponse, DeleteThesisResponse, ThesisStudentResponse } from '../../../models/thesis.model';

@Injectable({ providedIn: 'root' })
export class ThesisSettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/theses`;

  deleteThesis(thesisId: string): Observable<DeleteThesisResponse> {
    return this.http.delete<DeleteThesisResponse>(`${this.baseUrl}/${thesisId}`);
  }

  getThesisStudents(thesisId: string): Observable<ThesisStudentResponse[]> {
    return this.http.get<ThesisStudentResponse[]>(`${this.baseUrl}/${thesisId}/students`);
  }

  addStudent(thesisId: string, studentKeycloakId: string): Observable<ThesisResponse> {
    return this.http.post<ThesisResponse>(`${this.baseUrl}/${thesisId}/students/${studentKeycloakId}`, null);
  }

  removeStudent(thesisId: string, studentKeycloakId: string): Observable<ThesisResponse> {
    return this.http.delete<ThesisResponse>(`${this.baseUrl}/${thesisId}/students/${studentKeycloakId}`);
  }
}
