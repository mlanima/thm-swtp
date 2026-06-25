import { Component, inject, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { Subject, takeUntil, forkJoin, debounceTime, distinctUntilChanged, switchMap, catchError, of } from 'rxjs';
import { SearchService } from '../../services/search.service';
import { ProjectSearchResult } from '../../models/project-search-result.model';
import { UserSearchResult } from '../../models/user-search-result.model';
import { ProjectResultCard } from '../../components/project-result-card/project-result-card';
import { UserResultCard } from '../../components/user-result-card/user-result-card';
import { SearchInputComponent } from '../../components/search-input/search-input';

type Tab = 'all' | 'projects' | 'users';

const PAGE_SIZE = 20;
const PREVIEW_SIZE = 5;

@Component({
  selector: 'app-search-page',
  standalone: true,
  imports: [ProjectResultCard, UserResultCard, SearchInputComponent, TranslatePipe],
  templateUrl: './search-page.html',
})
export class SearchPage implements OnInit, OnDestroy {
  private readonly searchService = inject(SearchService);
  private readonly destroy$ = new Subject<void>();
  private readonly query$ = new Subject<string[]>();

  private currentQueries: string[] = [];

  readonly activeTab = signal<Tab>('all');
  readonly errorMessage = signal('');

  readonly projectResults = signal<ProjectSearchResult[]>([]);
  readonly userResults = signal<UserSearchResult[]>([]);
  readonly projects = this.projectResults;
  readonly users = this.userResults;

  readonly isLoading = signal(false);
  readonly currentQueriesCount = signal(0);

  // Preview shown on the "Alle" tab (first few results of each type)
  readonly previewProjects = signal<ProjectSearchResult[]>([]);
  readonly previewUsers = signal<UserSearchResult[]>([]);

  // Total match counts, used for tab badges and the results heading
  readonly projectsTotalCount = signal(0);
  readonly usersTotalCount = signal(0);

  readonly totalResults = computed(() => this.projectsTotalCount() + this.usersTotalCount());

  readonly projectsPageItems = signal<ProjectSearchResult[]>([]);
  readonly projectsPage = signal(0);
  readonly projectsTotalPages = signal(0);

  readonly usersPageItems = signal<UserSearchResult[]>([]);
  readonly usersPage = signal(0);
  readonly usersTotalPages = signal(0);

  readonly displayedProjects = computed(() =>
    this.activeTab() === 'projects' ? this.projectsPageItems() : this.previewProjects()
  );
  readonly displayedUsers = computed(() =>
    this.activeTab() === 'users' ? this.usersPageItems() : this.previewUsers()
  );

  readonly currentPage = computed(() =>
    this.activeTab() === 'users' ? this.usersPage() : this.projectsPage()
  );
  readonly currentTotalPages = computed(() =>
    this.activeTab() === 'users' ? this.usersTotalPages() : this.projectsTotalPages()
  );
  readonly pageNumbers = computed(() => Array.from({ length: this.currentTotalPages() }, (_, i) => i));

  ngOnInit(): void {
    this.query$
      .pipe(
        debounceTime(300),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
        switchMap(queries => {
          this.currentQueries = queries;
          if (queries.length === 0) {
             return of(null);
          }
          this.isLoading.set(true);
          return forkJoin({
            projects: this.searchService.searchProjectsPaged(queries, 0, PREVIEW_SIZE),
            users: this.searchService.searchUsersPaged(queries, 0, PREVIEW_SIZE),
          }).pipe(
            catchError(() => {
              this.errorMessage.set('SEARCH.ERROR_FAILED');
              return of(null);
            })
          );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(result => {
        if (result) {
          this.previewProjects.set(result.projects.content);
          this.previewUsers.set(result.users.content);
          this.projectsTotalCount.set(result.projects.totalElements);
          this.usersTotalCount.set(result.users.totalElements);

          if (this.currentQueries.length > 0) {
            const tab = this.activeTab();
            if (tab === 'projects' || tab === 'users') {
              this.loadTabPage(tab, 0);
              return;
            }
            this.resetPagedState();
          }
        }
        this.isLoading.set(false);
      });
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);

    if ((tab === 'projects' || tab === 'users') && this.currentQueries.length > 0) {
      const page = tab === 'projects' ? this.projectsPage() : this.usersPage();
      this.loadTabPage(tab, page);
    }
  }

  goToPage(page: number): void {
    const tab = this.activeTab();
    if (tab !== 'projects' && tab !== 'users') {
      return;
    }
    if (page < 0 || page >= this.currentTotalPages()) {
      return;
    }
    this.loadTabPage(tab, page);
  }

  onQueriesChange(queries: string[]): void {
    this.errorMessage.set('');
    this.currentQueriesCount.set(queries.length);

    if (queries.length === 0) {
      this.currentQueries = [];
      this.previewProjects.set([]);
      this.previewUsers.set([]);
      this.projectsTotalCount.set(0);
      this.usersTotalCount.set(0);
      this.resetPagedState();
      this.isLoading.set(false);
      this.query$.next([]);
      return;
    }

    this.query$.next(queries);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadTabPage(tab: 'projects' | 'users', page: number): void {
    this.isLoading.set(true);

    if (tab === 'projects') {
      this.searchService.searchProjectsPaged(this.currentQueries, page, PAGE_SIZE)
        .pipe(
          catchError(() => {
            this.errorMessage.set('SEARCH.ERROR_FAILED');
            return of(null);
          }),
          takeUntil(this.destroy$)
        )
        .subscribe(result => {
          if (result) {
            this.projectsPageItems.set(result.content);
            this.projectsPage.set(result.number);
            this.projectsTotalPages.set(result.totalPages);
          }
          this.isLoading.set(false);
        });
      return;
    }

    this.searchService.searchUsersPaged(this.currentQueries, page, PAGE_SIZE)
      .pipe(
        catchError(() => {
          this.errorMessage.set('SEARCH.ERROR_FAILED');
          return of(null);
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(result => {
        if (result) {
          this.usersPageItems.set(result.content);
          this.usersPage.set(result.number);
          this.usersTotalPages.set(result.totalPages);
        }
        this.isLoading.set(false);
      });
  }

  private resetPagedState(): void {
    this.projectsPageItems.set([]);
    this.projectsPage.set(0);
    this.projectsTotalPages.set(0);
    this.usersPageItems.set([]);
    this.usersPage.set(0);
    this.usersTotalPages.set(0);
  }
}
