export interface ProjectInviteResponse {
  id: string;
  projectId: string;
  projectName: string;
  projectUrl: string;
  invitedByUsername: string;
  invitedUserId: string;
  message: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}


export interface CreateProjectInviteRequest {
  invitedUserId: string;
  message?: string;
}
