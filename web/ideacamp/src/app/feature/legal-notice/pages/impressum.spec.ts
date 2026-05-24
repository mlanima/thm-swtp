import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { Impressum } from './impressum';

describe('Impressum', () => {
  let component: Impressum;
  let fixture: ComponentFixture<Impressum>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Impressum],
      providers: [{ provide: ActivatedRoute, useValue: {} }],
    }).compileComponents();

    fixture = TestBed.createComponent(Impressum);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
