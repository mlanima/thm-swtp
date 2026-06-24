export interface ProjectView {
  id: string;
  name: string;
  memberCount: number;
  ownerUsername: string;
  ownerInitials: string;
  createdAt: string;
  isPrivate: boolean;
}

export interface DeleteState {
  projectId: string;
  projectName: string;
}
