import { describe, it, expect } from 'vitest';
import { eventRegistry } from '../events/eventRegistry.js';

describe('eventRegistry', () => {
  it('contains POST_CREATED with sendPost job', () => {
    expect(eventRegistry.POST_CREATED.jobName).toBe('sendPost');
    expect(eventRegistry.POST_CREATED.defaultAttempts).toBe(5);
  });

  it('contains POST_UPDATED with editPost job', () => {
    expect(eventRegistry.POST_UPDATED.jobName).toBe('editPost');
    expect(eventRegistry.POST_UPDATED.defaultAttempts).toBe(3);
  });

  it('contains POST_DELETED with deletePost job', () => {
    expect(eventRegistry.POST_DELETED.jobName).toBe('deletePost');
    expect(eventRegistry.POST_DELETED.defaultAttempts).toBe(3);
  });

  it('contains PROJECT_INVITE with sendInvite job', () => {
    expect(eventRegistry.PROJECT_INVITE.jobName).toBe('sendInvite');
    expect(eventRegistry.PROJECT_INVITE.defaultAttempts).toBe(3);
  });

  it('contains PROJECT_EVENT with sendEvent job', () => {
    expect(eventRegistry.PROJECT_EVENT.jobName).toBe('sendEvent');
    expect(eventRegistry.PROJECT_EVENT.defaultAttempts).toBe(3);
  });

  it('every registered event has a schema', () => {
    for (const key of Object.keys(eventRegistry) as Array<keyof typeof eventRegistry>) {
      expect(eventRegistry[key].schema).toBeDefined();
    }
  });

  it('has exactly 5 event types', () => {
    expect(Object.keys(eventRegistry)).toHaveLength(5);
  });
});
