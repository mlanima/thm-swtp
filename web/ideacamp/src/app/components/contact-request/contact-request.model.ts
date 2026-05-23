/** Stores the needed contact request data which is displayed on the frontend.*/
export type ContactRequestStatus = 'Open' | 'Accepted' | 'Rejected';
export interface ContactRequest {
  id: string;
  senderId : string,
  senderName: string;
  projectId: string;
  projectName: string;
  message: string;
  status: ContactRequestStatus;
  date : string;
}
