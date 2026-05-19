import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegisterPassword } from './register-password';

describe('RegisterPassword', () => {
  let component: RegisterPassword;
  let fixture: ComponentFixture<RegisterPassword>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterPassword]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterPassword);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
