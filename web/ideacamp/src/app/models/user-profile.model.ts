/**
 * Represents user profile data returned by the backend
 *
 * The model is used by profile-related components and services to keep the
 * expected profile structure consistent across the frontend
 */
export interface UserProfileModel {
  /** Unique user identifier from Keycloak */
  keycloakId: string;

  /** Public username of the user */
  username: string;

  /** Optional email address of the user */
  email: string | null;

  /** Optional profile title */
  title: string | null;

  /** Optional location displayed on the user's profile */
  location: string | null;

  /** Number of followers the user has */
  followers: number;

  /** Optional about text written by the user */
  about: string | null;

  /** Optional experience description written by the user */
  experience: string | null;
}
