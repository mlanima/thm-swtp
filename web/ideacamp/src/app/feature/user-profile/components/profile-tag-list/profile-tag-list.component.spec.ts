import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfileTagListComponent } from './profile-tag-list.component';

describe('ProfileTagListComponent', () => {
  let component: ProfileTagListComponent;
  let fixture: ComponentFixture<ProfileTagListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileTagListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileTagListComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
