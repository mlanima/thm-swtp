export type ProfRequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface ModeratorProfRequest {
  id: string;
  requestingUserId: string;
  requestingUsername: string;
  email: string;
  text: string;
  createdAt: string;
  updatedAt: string;
  status: ProfRequestStatus;
}
