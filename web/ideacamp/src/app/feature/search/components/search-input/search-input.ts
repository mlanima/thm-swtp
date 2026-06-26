import { Component, ElementRef, output, viewChild, signal, computed, inject, DestroyRef } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap, catchError, of } from 'rxjs';
import { SearchService } from '../../services/search.service';

const SUGGESTED_TAGS_LIMIT = 10;

@Component({
  selector: 'app-search-input',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './search-input.html'
})
export class SearchInputComponent {
  readonly searchChange = output<string[]>();
  readonly searchInput = viewChild.required<ElementRef<HTMLInputElement>>('searchInput');

  private readonly searchService = inject(SearchService);
  private readonly suggestionQuery$ = new Subject<string>();

  readonly suggestedTags = signal<string[]>([]);
  readonly filteredSuggestedTags = computed(() => {
    const queries = this.queries();
    return this.suggestedTags().filter(tag => !queries.includes(tag));
  });

  queries = signal<string[]>([]);
  readonly removingQueries = signal<Set<string>>(new Set());
  readonly addingQueries = signal<Set<string>>(new Set());
  inputValue = '';

  constructor() {
    const sub = this.suggestionQuery$.pipe(
      debounceTime(200),
      distinctUntilChanged(),
      switchMap(query => this.searchService.searchTags(query, SUGGESTED_TAGS_LIMIT).pipe(catchError(() => of([]))))
    ).subscribe(tags => this.suggestedTags.set(tags));

    inject(DestroyRef).onDestroy(() => sub.unsubscribe());

    this.suggestionQuery$.next('');
  }

  addQuery(query: string) {
    const trimmed = query.trim();
    if (!trimmed) {
      this.inputValue = '';
      return;
    }

    if (this.removingQueries().has(trimmed)) {
      this.removingQueries.update(set => {
        const newSet = new Set(set);
        newSet.delete(trimmed);
        return newSet;
      });
    } else if (!this.queries().includes(trimmed)) {
      this.queries.update(q => [...q, trimmed]);
      this.addingQueries.update(set => {
        const newSet = new Set(set);
        newSet.add(trimmed);
        return newSet;
      });
      setTimeout(() => {
        this.addingQueries.update(set => {
          const newSet = new Set(set);
          newSet.delete(trimmed);
          return newSet;
        });
      }, 150);
    }

    this.inputValue = '';
    this.suggestionQuery$.next('');
    this.emitChange();
  }

  onInputChange() {
    this.suggestionQuery$.next(this.inputValue);
    this.emitChange();
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addQuery(this.inputValue);
    } else if (event.key === 'Backspace' && this.inputValue === '' && this.queries().length > 0) {
      const lastQuery = this.queries()[this.queries().length - 1];
      if (!this.removingQueries().has(lastQuery)) {
        this.removeQuery(lastQuery);
      }
    }
  }

  removeQuery(query: string) {
    this.removingQueries.update(set => {
      const newSet = new Set(set);
      newSet.add(query);
      return newSet;
    });

    setTimeout(() => {
      this.queries.update(q => q.filter(item => item !== query));
      this.removingQueries.update(set => {
        const newSet = new Set(set);
        newSet.delete(query);
        return newSet;
      });
      this.emitChange();
    }, 150);
  }

  focusInput() {
    this.searchInput().nativeElement.focus();
  }

  private emitChange() {
    const currentQueries = [...this.queries()];
    const trimmedInput = this.inputValue.trim();
    if (trimmedInput) {
        currentQueries.push(trimmedInput);
    }
    this.searchChange.emit(currentQueries);
  }
}
