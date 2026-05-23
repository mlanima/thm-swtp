import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContactRequestBox } from './contact-request-box';

describe('ContactRequestBox', () => {
  let component: ContactRequestBox;
  let fixture: ComponentFixture<ContactRequestBox>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContactRequestBox],
    }).compileComponents();

    fixture = TestBed.createComponent(ContactRequestBox);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
