import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { OAuthService } from 'angular-oauth2-oidc';

import { HeaderComponent } from './header.component';

import { provideRouter } from '@angular/router';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;
  const events$ = new Subject<void>();
  const oauthServiceMock = {
    events: events$.asObservable(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{ provide: OAuthService, useValue: oauthServiceMock }, provideRouter([])],
      imports: [HeaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
