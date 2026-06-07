export interface ProjectResponse {
  id: string;
  name: string;
  description: string;
  projectUrl: string;
  isPrivateProject: boolean;
  ownerId: string;
  memberIds: string[];
  favoriteCount: number;
  createdAt: string;
  updatedAt: string;
}
