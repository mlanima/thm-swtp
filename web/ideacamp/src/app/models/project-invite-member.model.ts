import { UserProfileModel } from './user-profile.model'

export type ProjectInviteMember = Pick<UserProfileModel, 'keycloakId' | 'username' | 'location' | 'title'>;
