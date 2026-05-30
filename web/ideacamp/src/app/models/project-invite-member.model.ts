import { UserProfileModel } from './user-profile.model'

export type ProjectInviteMember = Pick<UserProfileModel, 'keycloakId' | 'username' | 'email' | 'title'>;
