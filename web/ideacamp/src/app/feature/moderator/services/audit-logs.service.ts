import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../enviroments/enviroment.dev';
import { PageResponse } from '../../../models/page-response.model';
import { AuditLog } from '../audit-logs/models/audit-log.model';

export interface AuditLogSearchParams {
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class AuditLogsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/audit-logs`;

  getAuditLogs(params: AuditLogSearchParams): Observable<PageResponse<AuditLog>> {
    const httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString());

    return this.http.get<PageResponse<AuditLog>>(this.baseUrl, { params: httpParams });
  }
}
