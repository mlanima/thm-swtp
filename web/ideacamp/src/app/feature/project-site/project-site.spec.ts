import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectSite } from './project-site';

describe('ProjectSite', () => {
  let component: ProjectSite;
  let fixture: ComponentFixture<ProjectSite>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectSite],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectSite);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
