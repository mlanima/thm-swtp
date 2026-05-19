import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegisterUsername } from './register-username';

describe('RegisterUsername', () => {
  let component: RegisterUsername;
  let fixture: ComponentFixture<RegisterUsername>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterUsername]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterUsername);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
