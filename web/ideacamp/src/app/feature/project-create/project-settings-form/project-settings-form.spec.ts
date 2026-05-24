import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectSettingsForm } from './project-settings-form';

describe('ProjectSettingsForm', () => {
  let component: ProjectSettingsForm;
  let fixture: ComponentFixture<ProjectSettingsForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectSettingsForm],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectSettingsForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
