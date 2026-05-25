import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TagList } from './tag-list';

describe('TagList', () => {
  let component: TagList;
  let fixture: ComponentFixture<TagList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TagList],
    }).compileComponents();

    fixture = TestBed.createComponent(TagList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
