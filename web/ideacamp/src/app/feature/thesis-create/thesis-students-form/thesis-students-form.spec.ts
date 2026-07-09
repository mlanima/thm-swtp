import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../testing/translate-testing.provider';
import { ThesisStudentsForm } from './thesis-students-form';

describe('ThesisStudentsForm', () => {
  let component: ThesisStudentsForm;
  let fixture: ComponentFixture<ThesisStudentsForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ThesisStudentsForm],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ThesisStudentsForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
