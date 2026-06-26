export type UserStatus = 'ACTIVE' | 'BANNED';

export interface ManagedUser {
  keycloakId: string;
  username: string;
  email: string | null;
  isProfessor: boolean;
  status: UserStatus;
  banReason: string | null;
  bannedAt: string | null;
  createdAt: string;
}


export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
