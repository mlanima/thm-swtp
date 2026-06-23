import { describe, it, expect } from 'vitest';
import en from '../assets/i18n/en.json';
import de from '../assets/i18n/de.json';

interface NestedDict {
  [key: string]: string | NestedDict;
}

function getLeafKeys(obj: NestedDict, prefix = ''): string[] {
  const keys: string[] = [];
  for (const [key, value] of Object.entries(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    if (typeof value === 'string') {
      keys.push(fullKey);
    } else if (value && typeof value === 'object') {
      keys.push(...getLeafKeys(value, fullKey));
    }
  }
  return keys;
}

describe('i18n key integrity', () => {
  const enKeys = getLeafKeys(en);
  const deKeys = getLeafKeys(de);

  it('every key in en.json has a matching key in de.json', () => {
    const missing = enKeys.filter((k) => !deKeys.includes(k));
    expect(missing).toEqual([]);
  });

  it('every key in de.json has a matching key in en.json', () => {
    const extra = deKeys.filter((k) => !enKeys.includes(k));
    expect(extra).toEqual([]);
  });
});
