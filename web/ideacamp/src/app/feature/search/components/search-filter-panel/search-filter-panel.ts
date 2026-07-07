import { Component, input, output, computed } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectSearchFilters, UserSearchFilters } from '../../models/search-filters.model';

const MS_PER_DAY = 24 * 60 * 60 * 1000;

export type SearchFilterMode = 'projects' | 'users';
export type SearchFilters = ProjectSearchFilters | UserSearchFilters;
export type DateRangePreset = 'any' | '7d' | '30d' | '1y';

const PRESET_DAYS: Record<Exclude<DateRangePreset, 'any'>, number> = {
  '7d': 7,
  '30d': 30,
  '1y': 365,
};

@Component({
  selector: 'app-search-filter-panel',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './search-filter-panel.html',
})
export class SearchFilterPanel {
  readonly mode = input.required<SearchFilterMode>();
  readonly filters = input.required<SearchFilters>();
  readonly filtersChange = output<SearchFilters>();

  readonly activeDateRangePreset = computed<DateRangePreset>(() => {
    const createdAfter = this.filters().createdAfter;
    if (!createdAfter) {
      return 'any';
    }
    for (const preset of Object.keys(PRESET_DAYS) as Exclude<DateRangePreset, 'any'>[]) {
      if (createdAfter === this.presetDate(PRESET_DAYS[preset])) {
        return preset;
      }
    }
    return 'any';
  });

  get projectFilters(): ProjectSearchFilters {
    return this.filters() as ProjectSearchFilters;
  }

  get userFilters(): UserSearchFilters {
    return this.filters() as UserSearchFilters;
  }

  toggleHasOpenPositions(): void {
    this.emit({ hasOpenPositions: !this.projectFilters.hasOpenPositions });
  }

  toggleAllowJoinRequests(): void {
    this.emit({ allowJoinRequests: !this.projectFilters.allowJoinRequests });
  }

  toggleIsProfessor(): void {
    this.emit({ isProfessor: !this.userFilters.isProfessor });
  }

  setLocation(value: string): void {
    this.emit({ location: value.trim() || null });
  }

  setDateRangePreset(preset: DateRangePreset): void {
    const createdAfter = preset === 'any' ? null : this.presetDate(PRESET_DAYS[preset]);
    this.emit({ createdAfter, createdBefore: null });
  }

  private presetDate(daysAgo: number): string {
    return new Date(Date.now() - daysAgo * MS_PER_DAY).toISOString().slice(0, 10);
  }

  private emit(partial: Partial<ProjectSearchFilters> & Partial<UserSearchFilters>): void {
    this.filtersChange.emit({ ...this.filters(), ...partial } as SearchFilters);
  }
}
