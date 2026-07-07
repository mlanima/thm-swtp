export interface ProjectSearchFilters {
  hasOpenPositions: boolean;
  allowJoinRequests: boolean;
  createdAfter: string | null;
  createdBefore: string | null;
}

export interface UserSearchFilters {
  isProfessor: boolean;
  location: string | null;
  createdAfter: string | null;
  createdBefore: string | null;
}

export const EMPTY_PROJECT_FILTERS: ProjectSearchFilters = {
  hasOpenPositions: false,
  allowJoinRequests: false,
  createdAfter: null,
  createdBefore: null,
};

export const EMPTY_USER_FILTERS: UserSearchFilters = {
  isProfessor: false,
  location: null,
  createdAfter: null,
  createdBefore: null,
};
