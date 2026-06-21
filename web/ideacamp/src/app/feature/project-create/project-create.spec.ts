import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../testing/translate-testing.provider';
import { ProjectCreate } from './project-create';

describe('ProjectCreate', () => {
  let component: ProjectCreate;
  let fixture: ComponentFixture<ProjectCreate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectCreate],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectCreate);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
