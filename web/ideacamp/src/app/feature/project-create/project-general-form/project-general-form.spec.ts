import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../testing/translate-testing.provider';
import { ProjectGeneralForm } from './project-general-form';

describe('ProjectGeneralForm', () => {
  let component: ProjectGeneralForm;
  let fixture: ComponentFixture<ProjectGeneralForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectGeneralForm],
      providers: [
        provideTranslateTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectGeneralForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
