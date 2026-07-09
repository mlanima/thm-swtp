import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../enviroments/enviroment.dev';
import { ProjectFileModel, ProjectFileSchema } from '../../../models/project-file.model';
import { FileVisibility } from '../../../models/file-visibility.model';
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

  uploadFile(projectId: string, file: File, visibility: FileVisibility = 'PUBLIC'): Observable<ProjectFileModel> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('visibility', visibility);
    return this.http
      .post<unknown>(`${this.baseUrl}/${projectId}/files`, formData)
      .pipe(map((data) => ProjectFileSchema.parse(data)));
  }

  updateFileVisibility(projectId: string, fileId: string, visibility: FileVisibility): Observable<ProjectFileModel> {
    return this.http
      .patch<unknown>(`${this.baseUrl}/${projectId}/files/${fileId}`, { visibility })
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
