import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OpenPositionCard } from './open-position-card';

describe('OpenPositionCard', () => {
  let component: OpenPositionCard;
  let fixture: ComponentFixture<OpenPositionCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OpenPositionCard],
    }).compileComponents();

    fixture = TestBed.createComponent(OpenPositionCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
