import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../../testing/translate-testing.provider';
import { ProjectFiles } from './project-files';

describe('ProjectFiles', () => {
  let component: ProjectFiles;
  let fixture: ComponentFixture<ProjectFiles>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectFiles],
      providers: [
        provideTranslateTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectFiles);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});