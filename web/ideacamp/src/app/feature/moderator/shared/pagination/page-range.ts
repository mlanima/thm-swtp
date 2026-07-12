import { computed, Signal } from '@angular/core';

export interface PageRange {
  start: Signal<number>;
  end: Signal<number>;
}

export function createPageRange<T>(
  items: Signal<readonly T[]>,
  currentPage: Signal<number>,
  pageSize: number,
): PageRange {
  const start = computed(() => (items().length === 0 ? 0 : currentPage() * pageSize + 1));

  const end = computed(() => (items().length === 0 ? 0 : start() + items().length - 1));

  return { start, end };
}
