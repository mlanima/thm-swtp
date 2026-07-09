export interface ProjectView {
  id: string;
  name: string;
  memberCount: number;
  ownerUsername: string;
  ownerInitials: string;
  createdAt: string;
  createdAtShort: string;
  updatedAt: string;
  updatedAtShort: string;
  isPrivate: boolean;
}

export interface DeleteState {
  projectId: string;
  projectName: string;
}

/** Backend project fields used for the moderator project table sorting.*/
export type ManagedProjectSortField = 'name' | 'owner.username' | 'createdAt' | 'updatedAt' | 'isPrivateProject';

/** Sort direction accepted by the backend.*/
export type SortDirection = 'asc' | 'desc';
