export interface ProjectStatsResponse {
  contributors: number;
  views: number;
  likes: number;
  openPositions: number;
}

export interface ProjectResponse {
  id: string;
  name: string;
  shortDescription?: string;
  description: string;
  projectUrl: string;
  isPrivateProject: boolean;
  allowJoinRequests: boolean;
  ownerId: string;
  memberIds: string[];
  favoriteCount: number;
  stats: ProjectStatsResponse;
  createdAt: string;
  updatedAt: string;
}
