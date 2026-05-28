export interface ProjectResponse {
  id: string;
  name: string;
  description: string;
  projectUrl: string;
  isPrivateProject: boolean;
  ownerId: string;
  memberIds: string[];
  createdAt: string;
  updatedAt: string;
}
