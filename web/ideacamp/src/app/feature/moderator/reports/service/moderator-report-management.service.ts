import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../enviroments/enviroment.dev';
import {
  ManagedReport,
  ReportSearchParams,
  UpdateReportStatus,
} from '../models/report.model';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../../models/page-response.model';

@Injectable({ providedIn: 'root' })
export class ModeratorReportManagementService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/reports`;

  getReports(params: ReportSearchParams): Observable<PageResponse<ManagedReport>> {
    return this.http.get<PageResponse<ManagedReport>>(this.baseUrl, {
      params: this.buildReportParams(params),
    });
  }

  updateReportStatus(reportId: string, request: UpdateReportStatus): Observable<ManagedReport> {
    return this.http.patch <ManagedReport>(`${this.baseUrl}/${reportId}/status`, request);
  }

  private buildReportParams(params: ReportSearchParams): HttpParams {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString())
      .set('sort', `${params.sortField ?? 'createdAt'},${params.sortDirection ?? 'desc'}`);

    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }

    if (params.target) {
      httpParams = httpParams.set('target', params.target);
    }

    if (params.reason) {
      httpParams = httpParams.set('reason', params.reason);
    }

    if (params.query?.trim()) {
      httpParams = httpParams.set('query', params.query.trim());
    }

    return httpParams;
  }
}
