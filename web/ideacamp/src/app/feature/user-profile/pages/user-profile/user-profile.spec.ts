import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideTranslateTesting } from '../../../../testing/translate-testing.provider';
import { UserProfile } from './user-profile';
import { AuthService } from '../../../auth/auth.service';

describe('UserProfile', () => {
  let component: UserProfile;
  let fixture: ComponentFixture<UserProfile>;

  const authServiceMock = {
    username: () => '',
    isLoggedIn: () => false,
    isLoggingOut: () => false,
    waitUntilAuthReady: () => Promise.resolve(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserProfile],
      providers: [
        provideTranslateTesting(),
        provideRouter([]),
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserProfile);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
