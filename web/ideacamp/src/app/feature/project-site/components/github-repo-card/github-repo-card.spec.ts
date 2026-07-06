import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideTranslateTesting } from '../../../../testing/translate-testing.provider';
import { GithubRepoCard } from './github-repo-card';
import { ProjectGithubRepoService } from '../../services/project-github-repo.service';
import { GithubRepoCardModel } from '../../../../models/github-repo-card.model';

const repoCard: GithubRepoCardModel = {
  repoOwner: 'mlanima',
  repoName: 'thm-swtp',
  htmlUrl: 'https://github.com/mlanima/thm-swtp',
  description: 'IdeaCamp',
  stargazersCount: 7,
  forksCount: 2,
  languages: [
    { name: 'Java', percentage: 60 },
    { name: 'TypeScript', percentage: 40 },
  ],
  dataUnavailable: false,
};

class MockProjectGithubRepoService {
  getRepoCard() {
    return of(repoCard);
  }
}

describe('GithubRepoCard', () => {
  let component: GithubRepoCard;
  let fixture: ComponentFixture<GithubRepoCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GithubRepoCard],
      providers: [
        provideTranslateTesting(),
        { provide: ProjectGithubRepoService, useClass: MockProjectGithubRepoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GithubRepoCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('projectId', '00000000-0000-0000-0000-000000000000');
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load and display the linked repo card', () => {
    expect(component.card()).toEqual(repoCard);
    expect(component.isLoading()).toBe(false);
  });
});
