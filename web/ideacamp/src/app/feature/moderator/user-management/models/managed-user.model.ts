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


export type ManagedUserSortField = 'username' | 'email' | 'isProfessor' | 'createdAt' | 'bannedAt' | 'banReason' | 'status';

export type SortDirection = 'asc' | 'desc';
