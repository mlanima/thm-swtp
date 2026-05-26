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
    component.profile = { keycloakId: '1', username: 'test', email: null, title: null, location: null, followers: 1, about: null, experience: null }; // tosch: fixed quick&dirty 'nen broken test, brauche für cicd-test-integration fehlerfreie tests... nicht so behalten!
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
