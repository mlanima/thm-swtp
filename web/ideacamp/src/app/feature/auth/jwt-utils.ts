/**
 * Decodes the payload portion of a JWT (base64url → object).
 * Handles base64url alphabet by converting `-` → `+` and `_` → `/`
 * before calling `atob`, and pads the string to a multiple of 4.
 */
export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split('.')[1]
      .replace(/-/g, '+')
      .replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=');
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}
