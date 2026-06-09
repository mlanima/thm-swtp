import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditableTagListComponent } from './editable-tag-list.component';

describe('EditableTagListComponent', () => {
  let component: EditableTagListComponent;
  let fixture: ComponentFixture<EditableTagListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditableTagListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EditableTagListComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
