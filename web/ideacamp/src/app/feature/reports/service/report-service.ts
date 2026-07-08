import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../enviroments/enviroment.dev';
import { Observable } from 'rxjs';
import { CreateReportRequest } from '../models/report-create.model';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/reports`;

  createReport(request: CreateReportRequest): Observable<void> {
    return this.http.post<void>(this.baseUrl, request);
  }
}
