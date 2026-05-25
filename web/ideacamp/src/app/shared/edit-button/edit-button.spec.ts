import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditButton } from './edit-button';

describe('EditButton', () => {
  let component: EditButton;
  let fixture: ComponentFixture<EditButton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditButton],
    }).compileComponents();

    fixture = TestBed.createComponent(EditButton);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
