import type { ZodSchema } from 'zod';
import {
  PostCreatedSchema,
  PostUpdatedSchema,
  PostDeletedSchema,
  ProjectInviteSchema,
  ProjectEventSchema,
} from '../streams/validator.js';

/** Describes how a stream event maps to a BullMQ job. */
export interface EventConfig {
  /** Name of the BullMQ job handler. */
  jobName: string;
  /** How many times to retry before failing. */
  defaultAttempts: number;
  /** Zod schema to validate the incoming payload. */
  schema: ZodSchema;
}

/**
 * Single source of truth for stream event types.
 * Maps event strings to job names, retry counts, and validation schemas.
 */
export const eventRegistry = {
  POST_CREATED: {
    jobName: 'sendPost',
    defaultAttempts: 5,
    schema: PostCreatedSchema,
  },
  POST_UPDATED: {
    jobName: 'editPost',
    defaultAttempts: 3,
    schema: PostUpdatedSchema,
  },
  POST_DELETED: {
    jobName: 'deletePost',
    defaultAttempts: 3,
    schema: PostDeletedSchema,
  },
  PROJECT_INVITE: {
    jobName: 'sendInvite',
    defaultAttempts: 3,
    schema: ProjectInviteSchema,
  },
  PROJECT_EVENT: {
    jobName: 'sendEvent',
    defaultAttempts: 3,
    schema: ProjectEventSchema,
  },
} as const;

/** Any key from the registry — used as a type constraint elsewhere. */
export type StreamEventType = keyof typeof eventRegistry;
