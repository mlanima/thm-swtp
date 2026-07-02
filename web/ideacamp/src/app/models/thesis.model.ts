export interface ThesisResponse {
  id: string;
  title: string;
  thesisUrl: string;
  shortDescription?: string;
  description: string;
  supervisorKeycloakId: string;
  supervisorUsername: string;
  tags: string[];
  studentKeycloakIds: string[];
  createdAt: string;
  updatedAt: string;
}

export interface DeleteThesisResponse {
  thesisId: string;
  message: string;
}

export interface ThesisStudentResponse {
  keycloakId: string;
  username: string;
  email: string;
}
