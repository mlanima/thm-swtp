import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ProjectFileService } from './project-file.service';

describe('ProjectFileService', () => {
  let service: ProjectFileService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProjectFileService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ProjectFileService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
