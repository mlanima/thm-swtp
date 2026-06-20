import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../testing/translate-testing.provider';
import { ProjectMembersForm } from './project-members-form';

describe('ProjectMembersForm', () => {
  let component: ProjectMembersForm;
  let fixture: ComponentFixture<ProjectMembersForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectMembersForm],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectMembersForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
