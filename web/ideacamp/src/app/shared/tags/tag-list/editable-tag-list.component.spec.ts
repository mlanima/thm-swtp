import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../testing/translate-testing.provider';
import { EditableTagListComponent } from './editable-tag-list.component';

describe('EditableTagListComponent', () => {
  let component: EditableTagListComponent;
  let fixture: ComponentFixture<EditableTagListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditableTagListComponent],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EditableTagListComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
