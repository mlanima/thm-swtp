import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../testing/translate-testing.provider';
import { ThesisCreate } from './thesis-create';

describe('ThesisCreate', () => {
  let component: ThesisCreate;
  let fixture: ComponentFixture<ThesisCreate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ThesisCreate],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ThesisCreate);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
