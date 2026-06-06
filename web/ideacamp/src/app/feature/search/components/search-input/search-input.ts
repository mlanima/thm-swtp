import { Component, ElementRef, EventEmitter, Output, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-search-input',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './search-input.html'
})
export class SearchInputComponent {
  @Output() searchChange = new EventEmitter<string[]>();
  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

  queries = signal<string[]>([]);
  readonly removingQueries = signal<Set<string>>(new Set());
  readonly addingQueries = signal<Set<string>>(new Set());
  inputValue = '';

  popularTags = ['TypeScript', 'Node.js', 'API', 'Database', 'Frontend', 'Backend', 'UI/UX', 'DevOps', 'Mobile'];

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

    this.emitChange();
    this.inputValue = '';
  }

  onInputChange() {
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
    this.searchInput?.nativeElement.focus();
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
