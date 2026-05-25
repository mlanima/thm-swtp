import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LocationIcon } from './location-icon';

describe('LocationIcon', () => {
  let component: LocationIcon;
  let fixture: ComponentFixture<LocationIcon>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LocationIcon],
    }).compileComponents();

    fixture = TestBed.createComponent(LocationIcon);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
