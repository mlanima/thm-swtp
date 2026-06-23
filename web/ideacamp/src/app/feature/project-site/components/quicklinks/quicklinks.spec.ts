import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../../testing/translate-testing.provider';
import { Quicklinks } from './quicklinks';

describe('Quicklinks', () => {
  let component: Quicklinks;
  let fixture: ComponentFixture<Quicklinks>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Quicklinks],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Quicklinks);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
