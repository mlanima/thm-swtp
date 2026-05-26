import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, forkJoin, debounceTime, distinctUntilChanged, switchMap, catchError, of } from 'rxjs';
import { z } from 'zod';
import { SearchService } from '../../services/search.service';
import { ProjectSearchResult } from '../../models/project-search-result.model';
import { UserSearchResult } from '../../models/user-search-result.model';
import { ProjectResultCard } from '../../components/project-result-card/project-result-card';
import { UserResultCard } from '../../components/user-result-card/user-result-card';

type Tab = 'projects' | 'users';

const SearchInputSchema = z
  .string()
  .regex(/^[a-zA-ZäöüÄÖÜß0-9 ]*$/, { message: 'Only letters and numbers allowed!' })
  .refine(value => value.length === 0 || !value.startsWith(' '), {
    message: 'Search cannot start with a space.',
  });

@Component({
  selector: 'app-search-page',
  standalone: true,
  imports: [FormsModule, ProjectResultCard, UserResultCard],
  templateUrl: './search-page.html',
})
export class SearchPage implements OnInit, OnDestroy {
  private readonly searchService = inject(SearchService);
  private readonly destroy$ = new Subject<void>();
  private readonly query$ = new Subject<string>();

  searchTerm = '';
  readonly activeTab = signal<Tab>('projects');
  readonly errorMessage = signal('');
  readonly projects = signal<ProjectSearchResult[]>([]);
  readonly users = signal<UserSearchResult[]>([]);
  readonly isLoading = signal(false);

  ngOnInit(): void {
    this.query$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query =>
          forkJoin({
            projects: this.searchService.searchProjects(query),
            users: this.searchService.searchUsers(query),
          }).pipe(
            catchError(() => {
              this.errorMessage.set('Search failed. Please try again.');
              this.isLoading.set(false);
              return of(null);
            })
          )
        ),
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

  onSearchChange(): void {
    const result = SearchInputSchema.safeParse(this.searchTerm);

    if (!result.success) {
      this.errorMessage.set(result.error.issues[0]?.message ?? 'Invalid input.');
      this.projects.set([]);
      this.users.set([]);
      return;
    }

    this.errorMessage.set('');
    const query = result.data.trim();

    if (query === '') {
      this.projects.set([]);
      this.users.set([]);
      this.isLoading.set(false);
      return;
    }

    this.isLoading.set(true);
    this.query$.next(query);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
