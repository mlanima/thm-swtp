import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../enviroments/enviroment.dev';
import { ProjectFileModel, ProjectFileSchema } from '../../../models/project-file.model';
import { Observable, map } from 'rxjs';
import { z } from 'zod';

@Injectable({ providedIn: 'root' })
export class ProjectFileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/projects`;

  getProjectFiles(projectId: string): Observable<ProjectFileModel[]> {
    return this.http
      .get<unknown>(`${this.baseUrl}/${projectId}/files`)
      .pipe(map((data) => z.array(ProjectFileSchema).parse(data)));
  }

  uploadFile(projectId: string, file: File): Observable<ProjectFileModel> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<unknown>(`${this.baseUrl}/${projectId}/files`, formData)
      .pipe(map((data) => ProjectFileSchema.parse(data)));
  }

  deleteFile(projectId: string, fileId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/files/${fileId}`);
  }

  downloadFile(projectId: string, fileId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${projectId}/files/${fileId}/download`, {
      responseType: 'blob',
    });
  }
}
