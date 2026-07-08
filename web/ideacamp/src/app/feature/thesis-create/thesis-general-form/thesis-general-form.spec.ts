import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../testing/translate-testing.provider';
import { ThesisGeneralForm } from './thesis-general-form';

describe('ThesisGeneralForm', () => {
  let component: ThesisGeneralForm;
  let fixture: ComponentFixture<ThesisGeneralForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ThesisGeneralForm],
      providers: [
        provideTranslateTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ThesisGeneralForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
