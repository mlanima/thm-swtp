import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FollowersIcon } from './followers-icon';

describe('FollowersIcon', () => {
  let component: FollowersIcon;
  let fixture: ComponentFixture<FollowersIcon>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FollowersIcon],
    }).compileComponents();

    fixture = TestBed.createComponent(FollowersIcon);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
