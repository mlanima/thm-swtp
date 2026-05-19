import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegisterMail } from './register-mail';

describe('RegisterMail', () => {
  let component: RegisterMail;
  let fixture: ComponentFixture<RegisterMail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterMail]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterMail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
