import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../../testing/translate-testing.provider';
import { ProfileTagListComponent } from './profile-tag-list.component';

describe('ProfileTagListComponent', () => {
  let component: ProfileTagListComponent;
  let fixture: ComponentFixture<ProfileTagListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileTagListComponent],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileTagListComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
