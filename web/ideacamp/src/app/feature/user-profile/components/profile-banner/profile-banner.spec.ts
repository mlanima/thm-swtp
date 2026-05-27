import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfileBanner } from './profile-banner';

describe('ProfileBanner', () => {
  let component: ProfileBanner;
  let fixture: ComponentFixture<ProfileBanner>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileBanner],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileBanner);
    component = fixture.componentInstance;

    component.profile = {
      keycloakId: '',
      username: '',
      email: '',
      title: '',
      location: '',
      followers: 0,
      about: '',
      experience: ''
    };

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
