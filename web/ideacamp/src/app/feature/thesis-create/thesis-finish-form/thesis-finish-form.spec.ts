import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../testing/translate-testing.provider';
import { ThesisFinishForm } from './thesis-finish-form';

describe('ThesisFinishForm', () => {
  let component: ThesisFinishForm;
  let fixture: ComponentFixture<ThesisFinishForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ThesisFinishForm],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ThesisFinishForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
