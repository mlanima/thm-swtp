import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ProjectReadme } from './project-readme';
import { ProjectGithubRepoService } from '../../services/project-github-repo.service';
import { GithubReadmeModel } from '../../../../models/github-readme.model';

let readmeResponse: GithubReadmeModel | null = null;

class MockProjectGithubRepoService {
  getReadme() {
    return of(readmeResponse);
  }
}

describe('ProjectReadme', () => {
  let fixture: ComponentFixture<ProjectReadme>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectReadme],
      providers: [{ provide: ProjectGithubRepoService, useClass: MockProjectGithubRepoService }],
    }).compileComponents();
  });

  it('renders nothing when the README is unavailable', async () => {
    readmeResponse = null;

    fixture = TestBed.createComponent(ProjectReadme);
    fixture.componentRef.setInput('projectId', '00000000-0000-0000-0000-000000000000');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.html()).toBeNull();
    expect(fixture.nativeElement.textContent.trim()).toBe('');
  });

  it('renders sanitized content when the README is available', async () => {
    readmeResponse = {
      repoOwner: 'mlanima',
      repoName: 'thm-swtp',
      defaultBranch: 'main',
      markdown: '# Hello World',
      available: true,
    };

    fixture = TestBed.createComponent(ProjectReadme);
    fixture.componentRef.setInput('projectId', '00000000-0000-0000-0000-000000000000');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.html()).toContain('Hello World');
    expect(fixture.nativeElement.textContent).toContain('Hello World');
  });
});
