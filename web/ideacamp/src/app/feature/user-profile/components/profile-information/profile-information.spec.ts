import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfileInformation } from './profile-information';

describe('ProfileInformation', () => {
  let component: ProfileInformation;
  let fixture: ComponentFixture<ProfileInformation>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileInformation],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileInformation);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
