import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuccessComponent } from './success.component';

describe('SuccessComponent', () => {
  let component: SuccessComponent;
  let fixture: ComponentFixture<SuccessComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SuccessComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SuccessComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render success content', () => {
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Erfolgreich!');
    expect(text).toContain('Starten');
  });
});
