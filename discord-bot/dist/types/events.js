export var StreamEventType;
(function (StreamEventType) {
    StreamEventType["PostCreated"] = "POST_CREATED";
    StreamEventType["PostUpdated"] = "POST_UPDATED";
    StreamEventType["PostDeleted"] = "POST_DELETED";
    StreamEventType["ProjectInvite"] = "PROJECT_INVITE";
    StreamEventType["ProjectEvent"] = "PROJECT_EVENT";
})(StreamEventType || (StreamEventType = {}));
export var PlatformEventType;
(function (PlatformEventType) {
    PlatformEventType["DiscordMessageAssigned"] = "DISCORD_MESSAGE_ASSIGNED";
    PlatformEventType["DiscordMessageCreated"] = "DISCORD_MESSAGE_CREATED";
    PlatformEventType["DiscordMessageUpdated"] = "DISCORD_MESSAGE_UPDATED";
    PlatformEventType["DiscordMessageDeleted"] = "DISCORD_MESSAGE_DELETED";
    PlatformEventType["InviteResponse"] = "INVITE_RESPONSE";
    PlatformEventType["ChannelDisconnected"] = "CHANNEL_DISCONNECTED";
})(PlatformEventType || (PlatformEventType = {}));
//# sourceMappingURL=events.js.map