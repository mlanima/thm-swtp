export type ProjectPostStatus = 'ARCHIVED' | 'DRAFT' | 'PUBLISHED';

export type PostContentFormat = 'MARKDOWN' | 'PLAIN_TEXT';

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
  ownerUsername: string;
  memberIds: string[];
  favoriteCount: number;
  stats: ProjectStatsResponse;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectPostResponse {
  id: string;
  projectId: string;
  authorId: string;
  authorName: string;
  title: string;
  content: string;
  contentFormat: PostContentFormat;
  status: ProjectPostStatus;
  publishedAt: string | null;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectPostRequest {
  title: string;
  content: string;
  contentFormat: PostContentFormat;
  status: ProjectPostStatus;
}
