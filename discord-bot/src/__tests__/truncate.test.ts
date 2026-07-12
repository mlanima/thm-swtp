import { describe, it, expect } from 'vitest';
import { truncate, EMBED_TITLE_MAX, EMBED_DESCRIPTION_MAX } from '../bot/utils/truncate.js';

describe('truncate', () => {
  it('returns text unchanged when within limit', () => {
    expect(truncate('hello', 10)).toBe('hello');
  });

  it('returns text unchanged when exactly at limit', () => {
    expect(truncate('12345', 5)).toBe('12345');
  });

  it('truncates with ellipsis when exceeding limit', () => {
    expect(truncate('hello world', 5)).toBe('he...');
  });

  it('uses 3 chars for ellipsis suffix', () => {
    const result = truncate('x'.repeat(10), 5);
    expect(result).toBe('xx...');
    expect(result.length).toBe(5);
  });

  it('handles empty string', () => {
    expect(truncate('', 10)).toBe('');
  });

  it('handles single char string at limit 1', () => {
    expect(truncate('a', 1)).toBe('a');
  });

  it('handles limit smaller than ellipsis length (result may exceed limit)', () => {
    expect(truncate('ab', 2)).toBe('ab');
    expect(truncate('abc', 2)).toBe('ab...');
  });

  it('has EMBED_TITLE_MAX = 256', () => {
    expect(EMBED_TITLE_MAX).toBe(256);
  });

  it('has EMBED_DESCRIPTION_MAX = 4096', () => {
    expect(EMBED_DESCRIPTION_MAX).toBe(4096);
  });

  it('truncates title at 256 with ellipsis', () => {
    const long = 'a'.repeat(300);
    const result = truncate(long, EMBED_TITLE_MAX);
    expect(result.length).toBe(EMBED_TITLE_MAX);
    expect(result.endsWith('...')).toBe(true);
  });

  it('truncates description at 4096 with ellipsis', () => {
    const long = 'a'.repeat(5000);
    const result = truncate(long, EMBED_DESCRIPTION_MAX);
    expect(result.length).toBe(EMBED_DESCRIPTION_MAX);
    expect(result.endsWith('...')).toBe(true);
  });
});
