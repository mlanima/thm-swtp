import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';
import { ThesisResponse } from '../../../models/thesis.model';

@Injectable({ providedIn: 'root' })
export class MyThesesService {
  private readonly http = inject(HttpClient);

  getMyTheses(username: string): Observable<ThesisResponse[]> {
    return this.http.get<ThesisResponse[]>(
      `${environment.apiUrl}/v1/users/${encodeURIComponent(username)}/theses`,
    );
  }
}