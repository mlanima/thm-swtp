import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../../testing/translate-testing.provider';
import { MemberList } from './member-list';

describe('MemberList', () => {
  let component: MemberList;
  let fixture: ComponentFixture<MemberList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MemberList],
      providers: [
        provideTranslateTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MemberList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
