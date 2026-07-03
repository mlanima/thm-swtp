export type AuditLogAction =
  | 'PROJECT_DELETED'
  | 'PROJECT_POST_DELETED'
  | 'USER_BANNED'
  | 'USER_UNBANNED'
  | 'PROFESSOR_REQUEST_ACCEPTED'
  | 'PROFESSOR_REQUEST_REJECTED';

export type AuditLogTargetType =
  | 'PROJECT'
  | 'PROJECT_POST'
  | 'USER'
  | 'PROFESSOR_REQUEST';

export interface AuditLog {
  id: string;
  action: AuditLogAction;
  actorUserId: string;
  actorUsername: string | null;
  actorEmail: string | null;
  targetType: AuditLogTargetType;
  targetId: string;
  targetName: string | null;
  details: string | null;
  createdAt: string;
}
