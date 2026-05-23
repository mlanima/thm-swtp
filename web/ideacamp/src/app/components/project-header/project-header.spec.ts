import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectHeader } from './project-header';

describe('ProjectHeader', () => {
  let component: ProjectHeader;
  let fixture: ComponentFixture<ProjectHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectHeader],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectHeader);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
