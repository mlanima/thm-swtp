import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '../../feature/auth/auth.service';
import { SidebarService } from './sidebar.service';

import { SidebarComponent } from './sidebar.component';

describe('SidebarComponent', () => {
  let component: SidebarComponent;
  let fixture: ComponentFixture<SidebarComponent>;
  let sidebarService: SidebarService;
  let logoutCalls = 0;

  beforeEach(async () => {
    logoutCalls = 0;

    await TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { logout: () => logoutCalls++ } }],
      imports: [SidebarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SidebarComponent);
    component = fixture.componentInstance;
    sidebarService = TestBed.inject(SidebarService);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should close sidebar and call auth logout', () => {
    sidebarService.open();

    component.logout();

    expect(sidebarService.isOpen()).toBe(false);
    expect(logoutCalls).toBe(1);
  });
});
