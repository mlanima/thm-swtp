/** Discord embed title length limit. */
export const EMBED_TITLE_MAX = 256;
/** Discord embed description length limit. */
export const EMBED_DESCRIPTION_MAX = 4096;

/** Truncates text with an ellipsis if it exceeds `max` characters. */
export function truncate(text: string, max: number): string {
  if (text.length <= max) return text;
  return text.slice(0, max - 3) + '...';
}
