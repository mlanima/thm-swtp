import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideTranslateTesting } from '../../../../testing/translate-testing.provider';
import { TagList } from './tag-list';
import { ProjectTagService } from '../../services/project-tag.service';

class MockProjectTagService {
  getProjectTags() {
    return of([]);
  }

  addTag() {
    return of({ name: 'Test' });
  }

  deleteTag() {
    return of(void 0);
  }
}

describe('TagList', () => {
  let component: TagList;
  let fixture: ComponentFixture<TagList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TagList],
      providers: [provideTranslateTesting(), { provide: ProjectTagService, useClass: MockProjectTagService }],
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
