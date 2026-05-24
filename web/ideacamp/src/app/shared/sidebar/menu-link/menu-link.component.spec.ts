import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MenuLinkComponent } from './menu-link.component';

describe('MenuLinkComponent', () => {
  let component: MenuLinkComponent;
  let fixture: ComponentFixture<MenuLinkComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MenuLinkComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MenuLinkComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
