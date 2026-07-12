import { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import { truncate, EMBED_TITLE_MAX, EMBED_DESCRIPTION_MAX } from './truncate.js';

const NEUTRAL_COLOR = 0x99AAB5;

/** Builds an embed for a forum post — author, title, truncated content, and a link back to the platform. */
export function buildPostEmbed(
  title: string,
  content: string,
  authorName: string,
  platformUrl: string,
  authorAvatar?: string,
): EmbedBuilder {
  return new EmbedBuilder()
    .setAuthor({ name: authorName, iconURL: authorAvatar ?? undefined })
    .setTitle(truncate(title, EMBED_TITLE_MAX))
    .setDescription(truncate(content, EMBED_DESCRIPTION_MAX))
    .setColor(NEUTRAL_COLOR)
    .setURL(platformUrl)
    .setTimestamp();
}

/** Builds a minimal embed for project-level events (e.g. milestone reached). */
export function buildEventEmbed(projectName: string, message: string): EmbedBuilder {
  return new EmbedBuilder()
    .setDescription(`${projectName} — ${message}`)
    .setColor(NEUTRAL_COLOR)
    .setTimestamp();
}

/** Builds an embed announcing a project invitation in a DM. */
export function buildInviteEmbed(projectName: string, inviterName: string): EmbedBuilder {
  return new EmbedBuilder()
    .setTitle(`Project Invitation: ${projectName}`)
    .setDescription(`You have been invited by **${inviterName}** to join the project **${projectName}**.`)
    .setColor(NEUTRAL_COLOR)
    .setTimestamp();
}

/** Builds an action row with Accept / Decline buttons for a given invite. */
export function buildInviteActionRow(inviteId: string): ActionRowBuilder<ButtonBuilder> {
  const accept = new ButtonBuilder()
    .setCustomId(`invite_accept_${inviteId}`)
    .setLabel('Accept')
    .setStyle(ButtonStyle.Success);

  const decline = new ButtonBuilder()
    .setCustomId(`invite_decline_${inviteId}`)
    .setLabel('Decline')
    .setStyle(ButtonStyle.Danger);

  return new ActionRowBuilder<ButtonBuilder>().addComponents(accept, decline);
}
