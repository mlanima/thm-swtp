import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { TagList } from './tag-list';
import { ProjectTagService } from '../../services/project-tag.service';

class MockProjectTagService {
  getProjectTags() {
    return of([]);
  }

  addTag() {
    return of({ name: 'Test' });
  }
}

describe('TagList', () => {
  let component: TagList;
  let fixture: ComponentFixture<TagList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TagList],
      providers: [{ provide: ProjectTagService, useClass: MockProjectTagService }],
    }).compileComponents();

    fixture = TestBed.createComponent(TagList);
    component = fixture.componentInstance;
    component.projectId = '00000000-0000-0000-0000-000000000000';
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
