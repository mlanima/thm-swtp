import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
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
      providers: [
        provideRouter([{ path: '**', component: ProjectReadme }]),
        { provide: ProjectGithubRepoService, useClass: MockProjectGithubRepoService },
      ],
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

    expect(fixture.componentInstance.html()).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Hello World');
  });

  it('rewrites a same-page anchor so it navigates within the current page, not to the app root', async () => {
    readmeResponse = {
      repoOwner: 'mlanima',
      repoName: 'thm-swtp',
      defaultBranch: 'main',
      markdown: '<a name="readme-top"></a>\n\n# Title\n\n[back to top](#readme-top)',
      available: true,
    };

    await TestBed.inject(Router).navigateByUrl('/project/thm-swtp');

    fixture = TestBed.createComponent(ProjectReadme);
    fixture.componentRef.setInput('projectId', '00000000-0000-0000-0000-000000000000');
    fixture.detectChanges();
    await fixture.whenStable();

    const anchor = fixture.nativeElement.querySelector('a[href$="#readme-top"]') as HTMLAnchorElement;
    expect(anchor.getAttribute('href')).toBe('/project/thm-swtp#readme-top');
    // The Angular sanitizer strips bare <a name="..."> anchors — bypassSecurityTrustHtml
    // (since DOMPurify already sanitized this content) is what keeps this target intact.
    expect(fixture.nativeElement.querySelector('a[name="readme-top"]')).not.toBeNull();
  });
});
