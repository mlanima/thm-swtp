import { ComponentFixture, TestBed } from '@angular/core/testing';
import {UserSearchPick} from './user-search-pick';

describe('UserSearchPickComponent', () => {
  let component: UserSearchPick;
  let fixture: ComponentFixture<UserSearchPick>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserSearchPick],
    }).compileComponents();

    fixture = TestBed.createComponent(UserSearchPick);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
