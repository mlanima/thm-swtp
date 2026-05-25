import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SearchProject } from './search-project';

describe('SearchProject', () => {
  let component: SearchProject;
  let fixture: ComponentFixture<SearchProject>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchProject],
    }).compileComponents();

    fixture = TestBed.createComponent(SearchProject);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
