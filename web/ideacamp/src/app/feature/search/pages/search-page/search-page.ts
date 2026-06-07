import { Component, inject, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { Subject, takeUntil, forkJoin, debounceTime, distinctUntilChanged, switchMap, catchError, of } from 'rxjs';
import { SearchService } from '../../services/search.service';
import { ProjectSearchResult } from '../../models/project-search-result.model';
import { UserSearchResult } from '../../models/user-search-result.model';
import { ProjectResultCard } from '../../components/project-result-card/project-result-card';
import { UserResultCard } from '../../components/user-result-card/user-result-card';
import { SearchInputComponent } from '../../components/search-input/search-input';

type Tab = 'all' | 'projects' | 'users';

@Component({
  selector: 'app-search-page',
  standalone: true,
  imports: [ProjectResultCard, UserResultCard, SearchInputComponent],
  templateUrl: './search-page.html',
})
export class SearchPage implements OnInit, OnDestroy {
  private readonly searchService = inject(SearchService);
  private readonly destroy$ = new Subject<void>();
  private readonly query$ = new Subject<string[]>();

  readonly activeTab = signal<Tab>('all');
  readonly errorMessage = signal('');
  readonly projects = signal<ProjectSearchResult[]>([]);
  readonly users = signal<UserSearchResult[]>([]);
  readonly isLoading = signal(false);
  readonly currentQueriesCount = signal(0);

  readonly totalResults = computed(() => this.projects().length + this.users().length);

  ngOnInit(): void {
    this.query$
      .pipe(
        debounceTime(300),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
        switchMap(queries => {
          if (queries.length === 0) {
             return of({ projects: [], users: [] });
          }
          return forkJoin({
            projects: this.searchService.searchProjects(queries),
            users: this.searchService.searchUsers(queries),
          }).pipe(
            catchError(() => {
              this.errorMessage.set('Search failed. Please try again.');
              this.isLoading.set(false);
              return of(null);
            })
          );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(result => {
        if (result) {
          this.projects.set(result.projects);
          this.users.set(result.users);
          this.isLoading.set(false);
        }
      });
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
  }

  onQueriesChange(queries: string[]): void {
    this.errorMessage.set('');
    this.currentQueriesCount.set(queries.length);

    if (queries.length === 0) {
      this.projects.set([]);
      this.users.set([]);
      this.isLoading.set(false);
      this.query$.next([]);
      return;
    }

    this.isLoading.set(true);
    this.query$.next(queries);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
