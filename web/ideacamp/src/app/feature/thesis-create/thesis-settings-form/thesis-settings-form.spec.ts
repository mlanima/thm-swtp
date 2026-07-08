import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../testing/translate-testing.provider';
import { ThesisSettingsForm } from './thesis-settings-form';

describe('ThesisSettingsForm', () => {
  let component: ThesisSettingsForm;
  let fixture: ComponentFixture<ThesisSettingsForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ThesisSettingsForm],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ThesisSettingsForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
