import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../testing/translate-testing.provider';
import { WizardLayout } from './wizard-layout';

describe('WizardLayout', () => {
  let component: WizardLayout;
  let fixture: ComponentFixture<WizardLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WizardLayout],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WizardLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
