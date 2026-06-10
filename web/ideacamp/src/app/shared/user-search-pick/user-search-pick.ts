import { ChangeDetectorRef, Component, ElementRef, OnDestroy, ViewChild, inject, input, output, signal, AfterViewInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { SearchService } from '../../feature/search/services/search.service';
import { UserSearchResult } from '../../feature/search/models/user-search-result.model';

@Component({
  selector: 'app-user-search-pick',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './user-search-pick.html',
})
export class UserSearchPick implements OnDestroy, AfterViewInit {
  private readonly searchService = inject(SearchService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly searchTerms = new Subject<string>();
  private readonly searchSubscription: Subscription;

  excludedUserIds = input<string[]>([]);
  placeholder = input('Find users')
  emptyMessage = input('No users found');

  userSelected = output<UserSearchResult>();

  @ViewChild('userSearchInput') userSearchInput?: ElementRef<HTMLInputElement>

  searchQuery = signal('');
  isSearching = signal(false);
  hasSearched = signal(false);
  searchResults = signal<UserSearchResult[]>([]);

  constructor(){
    this.searchSubscription = this.createSearchSubscription();
  }

  ngOnDestroy(): void {
    this.searchSubscription.unsubscribe();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.userSearchInput?.nativeElement.focus();
    });
  }



  searchUser(query:string): void {
    this.searchQuery.set(query);
    const cleanedQuery = query.trim();

    if (cleanedQuery.length >= 2) {
      this.isSearching.set(true);
      this.hasSearched.set(true);
    } else {
      this.resetSearchResult();
    }

    this.searchTerms.next(query);
    this.changeDetectorRef.detectChanges();
  }

  selectUser(user: UserSearchResult): void {
    this.userSelected.emit(user);
  }



  private resetSearchResult(): void {
    this.isSearching.set(false);
    this.searchResults.set([]);
    this.hasSearched.set(false);
  }

  private searchUsers(query: string) {
    const cleanedQuery = query.trim();

    if (cleanedQuery.length < 2){
      return of([]);
    }
    return this.searchService.searchUsers([cleanedQuery]).pipe(
      catchError(() => of([])),
    );
  }

  private filterSearchResults(users: UserSearchResult[]): UserSearchResult[] {
    const cleanedQuery = this.searchQuery().trim().toLowerCase();
    const exclude = new Set(this.excludedUserIds());

    return users.filter( (user) =>
      !exclude.has(user.keycloakId) && user.username.toLowerCase().includes(cleanedQuery));
  }

  private createSearchSubscription(): Subscription {
    return this.searchTerms.pipe(
      debounceTime(250),
      distinctUntilChanged(),
      switchMap((query) => this.searchUsers(query)),
    )
      .subscribe((users: UserSearchResult[]) => {
        this.searchResults.set(this.filterSearchResults(users));
        this.isSearching.set(false);
        this.changeDetectorRef.detectChanges();
      });
  }

}
