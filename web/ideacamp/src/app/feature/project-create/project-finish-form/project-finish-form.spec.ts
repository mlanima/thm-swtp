import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectFinishForm } from './project-finish-form';

describe('ProjectFinishForm', () => {
  let component: ProjectFinishForm;
  let fixture: ComponentFixture<ProjectFinishForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectFinishForm],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectFinishForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
