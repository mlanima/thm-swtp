import { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, type TextChannel, type NewsChannel, type ThreadChannel } from 'discord.js';
import type { Job } from 'bullmq';
import { discordClient } from '../bot/client.js';
import { streamProducer } from '../streams/producer.js';
import { logger } from '../config/logger.js';
import { messagesSent } from '../metrics/index.js';
import type {
  PostCreatedPayload,
  PostUpdatedPayload,
  PostDeletedPayload,
  ProjectInvitePayload,
  ProjectEventPayload,
} from '../types/events.js';

const NEUTRAL_COLOR = 0x99AAB5;

type SendableChannel = TextChannel | NewsChannel | ThreadChannel;

function resolveSendableChannel(channelId: string): SendableChannel | null {
  const channel = discordClient.channels.resolve(channelId);
  if (!channel) return null;
  if (channel.isDMBased()) return channel as unknown as SendableChannel;
  if (channel.isTextBased() && 'send' in channel) return channel as SendableChannel;
  return null;
}

export async function handleSendPost(job: Job<PostCreatedPayload>): Promise<void> {
  const { postId, channelId, content, authorName, authorAvatar, platformUrl } = job.data;

  const channel = resolveSendableChannel(channelId);
  if (!channel) {
    throw new Error(`Channel ${channelId} not found or not sendable`);
  }

  const embed = new EmbedBuilder()
    .setAuthor({ name: authorName, iconURL: authorAvatar ?? undefined })
    .setDescription(content.length > 4096 ? content.slice(0, 4093) + '...' : content)
    .setColor(NEUTRAL_COLOR)
    .setURL(platformUrl)
    .setTimestamp();

  const message = await channel.send({ embeds: [embed] });
  messagesSent.inc({ status: 'success' });

  const guildId = 'guildId' in channel ? (channel as { guildId?: string }).guildId : undefined;

  await streamProducer.discordMessageAssigned({
    postId,
    discordMsgId: message.id,
    channelId,
    guildId,
  });

  logger.info({ postId, discordMsgId: message.id }, 'post sent to discord');
}

export async function handleEditPost(job: Job<PostUpdatedPayload>): Promise<void> {
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
    .setDescription(content.length > 4096 ? content.slice(0, 4093) + '...' : content);

  await message.edit({ embeds: [updated] });
  messagesSent.inc({ status: 'edit' });

  logger.info({ discordMsgId }, 'post edited on discord');
}

export async function handleDeletePost(job: Job<PostDeletedPayload>): Promise<void> {
  const { discordMsgId, channelId } = job.data;

  const channel = resolveSendableChannel(channelId);
  if (!channel) return;

  try {
    const message = await channel.messages.fetch(discordMsgId);
    await message.delete();
    logger.info({ discordMsgId }, 'post deleted from discord');
  } catch (err: unknown) {
    if (err instanceof Error && err.message.includes('10008')) {
      logger.warn({ discordMsgId }, 'message already deleted, ignoring');
    } else {
      throw err;
    }
  }
}

export async function handleSendInvite(job: Job<ProjectInvitePayload>): Promise<void> {
  const { inviteId, targetDiscordId, projectName, inviterName } = job.data;

  const user = await discordClient.users.fetch(targetDiscordId).catch(() => null);
  if (!user) {
    logger.warn({ targetDiscordId }, 'cannot fetch user for invite DM');
    return;
  }

  const embed = new EmbedBuilder()
    .setTitle(`Project Invitation: ${projectName}`)
    .setDescription(`You have been invited by **${inviterName}** to join the project **${projectName}**.`)
    .setColor(NEUTRAL_COLOR)
    .setTimestamp();

  const accept = new ButtonBuilder()
    .setCustomId(`invite_accept_${inviteId}`)
    .setLabel('Accept')
    .setStyle(ButtonStyle.Success);

  const decline = new ButtonBuilder()
    .setCustomId(`invite_decline_${inviteId}`)
    .setLabel('Decline')
    .setStyle(ButtonStyle.Danger);

  const row = new ActionRowBuilder<ButtonBuilder>().addComponents(accept, decline);

  try {
    await user.send({ embeds: [embed], components: [row] });
    messagesSent.inc({ status: 'dm' });
    logger.info({ inviteId, targetDiscordId }, 'invite DM sent');
  } catch (err: unknown) {
    if (err instanceof Error && err.message.includes('50007')) {
      logger.warn({ targetDiscordId }, 'cannot DM user (blocked or no DMs)');
    } else {
      throw err;
    }
  }
}

export async function handleSendEvent(job: Job<ProjectEventPayload>): Promise<void> {
  const { channelId, projectName, message, eventType } = job.data;

  const channel = resolveSendableChannel(channelId);
  if (!channel) {
    throw new Error(`Channel ${channelId} not found or not sendable`);
  }

  const embed = new EmbedBuilder()
    .setDescription(`${projectName} — ${message}`)
    .setColor(NEUTRAL_COLOR)
    .setTimestamp();

  await channel.send({ embeds: [embed] });
  messagesSent.inc({ status: 'event' });

  logger.info({ eventType, channelId }, 'event notification sent');
}
