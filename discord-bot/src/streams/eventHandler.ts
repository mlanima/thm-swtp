import { EmbedBuilder, DiscordAPIError } from 'discord.js';
import type { Job } from 'bullmq';
import type { Client } from 'discord.js';
import type { Logger } from 'pino';
import type { Counter } from 'prom-client';
import { discordClient } from '../bot/client.js';
import { streamProducer } from '../streams/producer.js';
import { logger } from '../config/logger.js';
import { messagesSent } from '../metrics/index.js';
import { truncate, EMBED_DESCRIPTION_MAX } from '../bot/utils/truncate.js';
import { resolveSendableChannel } from '../bot/services/channelService.js';
import {
  buildPostEmbed,
  buildEventEmbed,
  buildInviteEmbed,
  buildInviteActionRow,
} from '../bot/utils/embedBuilder.js';
import type {
  PostCreatedPayload,
  PostUpdatedPayload,
  PostDeletedPayload,
  ProjectInvitePayload,
  ProjectEventPayload,
} from '../types/events.js';

/** Dependencies the event handlers need — makes them testable via factory injection. */
export interface EventHandlerDeps {
  discordClient: Client;
  streamProducer: {
    discordMessageAssigned: (payload: { postId: string; discordMsgId: string; channelId: string; guildId?: string }) => Promise<void>;
  };
  logger: Logger;
  messagesSent: Counter<string>;
}

/** Factory that creates event handlers with injected dependencies. Each handler maps to a BullMQ job name. */
export function createEventHandlers(deps: EventHandlerDeps) {
  const { discordClient, streamProducer, logger, messagesSent } = deps;

  /** Sends a new post embed to the configured Discord channel. */
  async function handleSendPost(job: Job<PostCreatedPayload>): Promise<void> {
    const { postId, channelId, content, title, authorName, authorAvatar, platformUrl } = job.data;

    const channel = resolveSendableChannel(channelId);
    if (!channel) {
      throw new Error(`Channel ${channelId} not found or not sendable`);
    }

    const embed = buildPostEmbed(title, content, authorName, platformUrl, authorAvatar);

    const message = await channel.send({ embeds: [embed] });
    messagesSent.inc({ status: 'success' });

    // Tell the platform which Discord message ID was assigned to this post
    const guildId = 'guildId' in channel ? (channel as { guildId?: string }).guildId : undefined;

    await streamProducer.discordMessageAssigned({
      postId,
      discordMsgId: message.id,
      channelId,
      guildId,
    });

    logger.info({ postId, discordMsgId: message.id }, 'post sent to discord');
  }

  /** Edits an existing Discord embed (updates description). */
  async function handleEditPost(job: Job<PostUpdatedPayload>): Promise<void> {
    const { discordMsgId, channelId, content } = job.data;

    const channel = resolveSendableChannel(channelId);
    if (!channel) {
      throw new Error(`Channel ${channelId} not found or not sendable`);
    }

    const message = await channel.messages.fetch(discordMsgId);
    const embed = message.embeds[0];
    if (!embed) {
      throw new Error(`Message ${discordMsgId} has no embed to edit`);
    }

    const updated = EmbedBuilder.from(embed)
      .setDescription(truncate(content, EMBED_DESCRIPTION_MAX));

    await message.edit({ embeds: [updated] });
    messagesSent.inc({ status: 'edit' });

    logger.info({ discordMsgId }, 'post edited on discord');
  }

  /** Deletes a Discord embed. Silently ignores if the message was already gone (error 10008). */
  async function handleDeletePost(job: Job<PostDeletedPayload>): Promise<void> {
    const { discordMsgId, channelId } = job.data;

    const channel = resolveSendableChannel(channelId);
    if (!channel) return;

    try {
      const message = await channel.messages.fetch(discordMsgId);
      await message.delete();
      logger.info({ discordMsgId }, 'post deleted from discord');
    } catch (err: unknown) {
      // Unknown Message (10008) — already deleted, nothing to do
      if (err instanceof DiscordAPIError && err.code === 10008) {
        logger.warn({ discordMsgId }, 'message already deleted, ignoring');
      } else {
        throw err;
      }
    }
  }

  /** Sends a project-invite DM with Accept / Decline buttons. */
  async function handleSendInvite(job: Job<ProjectInvitePayload>): Promise<void> {
    const { inviteId, targetDiscordId, projectName, inviterName } = job.data;

    const user = await discordClient.users.fetch(targetDiscordId).catch(() => null);
    if (!user) {
      logger.warn({ targetDiscordId }, 'cannot fetch user for invite DM');
      return;
    }

    const embed = buildInviteEmbed(projectName, inviterName);
    const row = buildInviteActionRow(inviteId);

    try {
      await user.send({ embeds: [embed], components: [row] });
      messagesSent.inc({ status: 'dm' });
      logger.info({ inviteId, targetDiscordId }, 'invite DM sent');
    } catch (err: unknown) {
      // Cannot send messages to this user (50007) — blocked or DMs disabled
      if (err instanceof DiscordAPIError && err.code === 50007) {
        logger.warn({ targetDiscordId }, 'cannot DM user (blocked or no DMs)');
      } else {
        throw err;
      }
    }
  }

  /** Sends a generic project event embed (milestone, status update, etc.). */
  async function handleSendEvent(job: Job<ProjectEventPayload>): Promise<void> {
    const { channelId, projectName, message, eventType } = job.data;

    const channel = resolveSendableChannel(channelId);
    if (!channel) {
      throw new Error(`Channel ${channelId} not found or not sendable`);
    }

    const embed = buildEventEmbed(projectName, message);

    await channel.send({ embeds: [embed] });
    messagesSent.inc({ status: 'event' });

    logger.info({ eventType, channelId }, 'event notification sent');
  }

  return {
    handleSendPost,
    handleEditPost,
    handleDeletePost,
    handleSendInvite,
    handleSendEvent,
  };
}

export type EventHandlers = ReturnType<typeof createEventHandlers>;

/** Default handler instance wired to the real Discord client and stream producer. */
export const eventHandlers = createEventHandlers({
  discordClient,
  streamProducer,
  logger,
  messagesSent,
});
