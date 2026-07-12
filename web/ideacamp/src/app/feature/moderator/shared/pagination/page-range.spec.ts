import { signal } from '@angular/core';
import { createPageRange } from './page-range';

describe('createPageRange', () => {
  const PAGE_SIZE = 10;

  it('should return 0–0 for an empty page', () => {
    const items = signal<string[]>([]);
    const currentPage = signal(0);

    const range = createPageRange(items, currentPage, PAGE_SIZE);

    expect(range.start()).toBe(0);
    expect(range.end()).toBe(0);
  });

  it('should return 1–10 for a full first page', () => {
    const items = signal(Array.from({ length: 10 }, (_, index) => `item-${index}`));
    const currentPage = signal(0);

    const range = createPageRange(items, currentPage, PAGE_SIZE);

    expect(range.start()).toBe(1);
    expect(range.end()).toBe(10);
  });

  it('should return 11–20 for a full second page', () => {
    const items = signal(Array.from({ length: 10 }, (_, index) => `item-${index}`));
    const currentPage = signal(1);

    const range = createPageRange(items, currentPage, PAGE_SIZE);

    expect(range.start()).toBe(11);
    expect(range.end()).toBe(20);
  });

  it('should use the actual item count for an incomplete page', () => {
    const items = signal(Array.from({ length: 5 }, (_, index) => `item-${index}`));
    const currentPage = signal(2);

    const range = createPageRange(items, currentPage, PAGE_SIZE);

    expect(range.start()).toBe(21);
    expect(range.end()).toBe(25);
  });

  it('should update when the page and items change', () => {
    const items = signal(Array.from({ length: 10 }, (_, index) => `item-${index}`));
    const currentPage = signal(0);

    const range = createPageRange(items, currentPage, PAGE_SIZE);

    expect(range.start()).toBe(1);
    expect(range.end()).toBe(10);

    currentPage.set(1);
    items.set(Array.from({ length: 4 }, (_, index) => `item-${index}`));

    expect(range.start()).toBe(11);
    expect(range.end()).toBe(14);
  });
});
