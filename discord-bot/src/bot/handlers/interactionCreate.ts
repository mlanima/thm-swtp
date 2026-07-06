import { Events, type Client, type Interaction } from 'discord.js';
import { streamProducer } from '../../streams/producer.js';
import { logger } from '../../config/logger.js';

const INVITE_ACCEPT_PREFIX = 'invite_accept_';
const INVITE_DECLINE_PREFIX = 'invite_decline_';

export function registerInteractionCreateHandler(client: Client): void {
  client.on(Events.InteractionCreate, async (interaction: Interaction) => {
    if (!interaction.isButton()) return;

    const customId = interaction.customId;

    if (customId.startsWith(INVITE_ACCEPT_PREFIX)) {
      const inviteId = customId.slice(INVITE_ACCEPT_PREFIX.length);
      await streamProducer.inviteResponse({ inviteId, response: 'ACCEPTED' });

      await interaction.update({
        content: 'Invitation accepted!',
        embeds: [],
        components: [],
      });

      logger.info({ inviteId, userId: interaction.user.id }, 'invite accepted via discord');
    } else if (customId.startsWith(INVITE_DECLINE_PREFIX)) {
      const inviteId = customId.slice(INVITE_DECLINE_PREFIX.length);
      await streamProducer.inviteResponse({ inviteId, response: 'DECLINED' });

      await interaction.update({
        content: 'Invitation declined.',
        embeds: [],
        components: [],
      });

      logger.info({ inviteId, userId: interaction.user.id }, 'invite declined via discord');
    }
  });
}
