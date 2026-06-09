export interface JoinRequest {
  id: string;
  userId: string;
  name: string;
  email: string;
  initials: string;
  avatarColor: string;
  message: string;
  requestDate: string;
}

export interface ProjectMember {
  id: string;
  name: string;
  email: string;
  initials: string;
  avatarColor: string;
  role: 'Owner' | 'Member';
  //joinedDate: string;
}

export interface PrivacySettings {
  isPublic: boolean;
  allowJoinRequests: boolean;
}

export interface ProjectMemberResponse {
  keycloakId: string;
  username: string;
  email: string;
}
