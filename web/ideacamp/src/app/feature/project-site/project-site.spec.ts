import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { signal } from '@angular/core';
import { provideTranslateTesting } from '../../testing/translate-testing.provider';
import { ProjectSite } from './project-site';
import { ProjectService } from './project.service';
import { AuthService } from '../auth/auth.service';

describe('ProjectSite', () => {
  let component: ProjectSite;
  let fixture: ComponentFixture<ProjectSite>;

  const projectServiceMock = {
    getProjectByUrl: () => ({ subscribe: () => void 0 }),
    updateProject: () => ({ subscribe: () => void 0 }),
  };

  const authServiceMock = {
    user: signal(null),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectSite],
      providers: [
        provideTranslateTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => null,
              },
            },
          },
        },
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectSite);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
