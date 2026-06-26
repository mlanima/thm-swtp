import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfessorRequestComponent } from './professor-request.component';

describe('ProfessorRequestComponent', () => {
  let component: ProfessorRequestComponent;
  let fixture: ComponentFixture<ProfessorRequestComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfessorRequestComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfessorRequestComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
