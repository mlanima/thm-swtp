import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthPanelComponent } from './auth-panel.component';

describe('AuthPanelComponent', () => {
  let component: AuthPanelComponent;
  let fixture: ComponentFixture<AuthPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AuthPanelComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
