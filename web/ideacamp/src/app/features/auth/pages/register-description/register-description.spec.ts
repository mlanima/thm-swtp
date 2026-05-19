import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegisterDescription } from './register-description';

describe('RegisterDescription', () => {
  let component: RegisterDescription;
  let fixture: ComponentFixture<RegisterDescription>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterDescription]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterDescription);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
