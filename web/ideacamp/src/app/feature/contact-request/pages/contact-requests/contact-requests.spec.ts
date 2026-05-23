import { ComponentFixture, TestBed } from '@angular/core/testing';

import {ContactRequests} from './contact-requests';

describe('ContactRequests', () => {
  let component: ContactRequests;
  let fixture: ComponentFixture<ContactRequests>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContactRequests],
    }).compileComponents();

    fixture = TestBed.createComponent(ContactRequests);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
