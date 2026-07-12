import { describe, it, expect } from 'vitest';
import {
  buildPostEmbed,
  buildEventEmbed,
  buildInviteEmbed,
  buildInviteActionRow,
} from '../bot/utils/embedBuilder.js';

describe('buildPostEmbed', () => {
  it('creates an embed with author, title, description, url and timestamp', () => {
    const embed = buildPostEmbed('My Title', 'Content', 'Alice', 'https://example.com/p/1', 'https://cdn.example.com/avatar.png');

    expect(embed.data.author).toEqual({ name: 'Alice', icon_url: 'https://cdn.example.com/avatar.png' });
    expect(embed.data.title).toBe('My Title');
    expect(embed.data.description).toBe('Content');
    expect(embed.data.url).toBe('https://example.com/p/1');
    expect(embed.data.timestamp).toBeDefined();
    expect(embed.data.color).toBe(0x99AAB5);
  });

  it('omits author icon when avatar is not provided', () => {
    const embed = buildPostEmbed('Title', 'Content', 'Bob', 'https://example.com');

    expect(embed.data.author).toEqual({ name: 'Bob' });
  });

  it('truncates long title', () => {
    const longTitle = 'a'.repeat(300);
    const embed = buildPostEmbed(longTitle, 'Content', 'Alice', 'https://example.com');

    expect(embed.data.title?.length).toBe(256);
    expect(embed.data.title?.endsWith('...')).toBe(true);
  });

  it('truncates long description', () => {
    const longContent = 'a'.repeat(5000);
    const embed = buildPostEmbed('Title', longContent, 'Alice', 'https://example.com');

    expect(embed.data.description?.length).toBe(4096);
    expect(embed.data.description?.endsWith('...')).toBe(true);
  });
});

describe('buildEventEmbed', () => {
  it('creates an embed with project name and message', () => {
    const embed = buildEventEmbed('My Project', 'A new member joined!');

    expect(embed.data.description).toBe('My Project — A new member joined!');
    expect(embed.data.timestamp).toBeDefined();
    expect(embed.data.color).toBe(0x99AAB5);
  });
});

describe('buildInviteEmbed', () => {
  it('creates an embed with invitation text', () => {
    const embed = buildInviteEmbed('My Project', 'Alice');

    expect(embed.data.title).toBe('Project Invitation: My Project');
    expect(embed.data.description).toContain('Alice');
    expect(embed.data.description).toContain('My Project');
    expect(embed.data.timestamp).toBeDefined();
    expect(embed.data.color).toBe(0x99AAB5);
  });
});

describe('buildInviteActionRow', () => {
  it('creates a row with accept and decline buttons', () => {
    const row = buildInviteActionRow('invite-123');
    const json = row.toJSON();

    expect(json.type).toBe(1);
    expect(json.components).toHaveLength(2);

    const [accept, decline] = json.components as unknown as Array<Record<string, unknown>>;
    expect(accept.custom_id).toBe('invite_accept_invite-123');
    expect(accept.label).toBe('Accept');
    expect(accept.style).toBe(3);
    expect(decline.custom_id).toBe('invite_decline_invite-123');
    expect(decline.label).toBe('Decline');
    expect(decline.style).toBe(4);
  });
});
