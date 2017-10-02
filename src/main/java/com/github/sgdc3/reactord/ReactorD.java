package com.github.sgdc3.reactord;

import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.DiscordApiBuilder;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.channels.ServerTextChannel;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.MessageHistory;
import de.btobastian.javacord.entities.message.Reaction;
import de.btobastian.javacord.entities.message.emoji.CustomEmoji;
import de.btobastian.javacord.entities.message.emoji.Emoji;
import de.btobastian.javacord.entities.message.emoji.impl.ImplUnicodeEmoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

public final class ReactorD {

    private final static String POLL_COMMAND = "poll:";

    private Logger logger;
    private Set<Long> pools;

    private DiscordApi discordApi;

    public ReactorD(String token, String instanceName) {
        logger = LoggerFactory.getLogger(instanceName);
        pools = new HashSet<>();

        logger.info("Welcome to ReactorD, connecting with token = " + token);

        try {
            discordApi = new DiscordApiBuilder().setToken(token).login().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("A fatal error occurred.", e);
            return;
        }

        logger.info("Loading channel data...");
        for (Server server : discordApi.getServers()) {
            for (ServerTextChannel channel : server.getTextChannels()) {
                logger.info("LOAD > Server: " + server.getName() + " > Channel: " + channel.getName());
                try {
                    MessageHistory history = channel.getHistory(100).get();
                    for (Message message : history.getMessages()) {
                        processMessage(message);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    logger.warn("Error while loading data for channel " + channel.getName() + "! Maybe missing permissions?");
                }
            }
        }
        logger.info("Retrieved " + pools.size() + " polls!");

        // register listeners
        discordApi.addMessageCreateListener(messageCreateEvent -> processMessage(messageCreateEvent.getMessage()));

        discordApi.addMessageDeleteListener(event -> event.requestMessage().thenAccept(message -> {
            if (pools.remove(message.getId())) {
                logger.debug("Removing poll. (Question:" + getQuestion(message.getContent()) + ")");
            }
        }));

        discordApi.addReactionAddListener(event -> event.requestMessage().thenAccept(message -> {
            if (!event.getReaction().isPresent()) {
                throw new IllegalStateException("Reaction value was null!"); // Should be impossible
            }
            processReaction(event.getReaction().get(), event.getUser());
        }));
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid arguments...");
            return;
        }
        String token = args[0];
        System.out.println("Starting bot application with token " + token);
        new ReactorD(token, "ReactorD");
    }

    private void processReaction(Reaction reaction, User user) {
        long millis = System.currentTimeMillis();
        if (user.isYourself()) {
            return;
        }
        Message message = reaction.getMessage();
        logger.debug("Processing reaction. (User:" + user.getName() + ", Question:" + getQuestion(message.getContent()) + ")");

        if (!pools.contains(message.getId())) {
            return;
        }

        Emoji emoji = reaction.getEmoji();
        logger.debug("Valid poll answer! (User:" + user.getName() + ", Question:" + getQuestion(message.getContent()) + ", Reaction: " + emoji.toString() + ")");

        List<Reaction> reactions = message.getReactions();
        for (Reaction currentReaction : reactions) {
            Emoji currentEmoji = currentReaction.getEmoji();
            if (emoji.equals(currentEmoji)) {
                continue;
            }
            currentReaction.removeUser(user);
        }

        /* Time expensive
        try {
            List<Reaction> reactions = message.getReactions();
            for (Reaction currentReaction : reactions) {
                Emoji currentEmoji = currentReaction.getEmoji();
                if (emoji.equals(currentEmoji)) {
                    continue;
                }
                currentReaction.removeUser(user);
                List<User> currentUsers = currentReaction.getUsers().get();
                if (currentUsers.contains(user)) {
                    logger.debug("Removing previous answer. (User:" + user.getName() + ", Question:" + getQuestion(message.getContent()) + ", Reaction: " + currentEmoji.toString() + ")");
                    currentReaction.removeUser(user);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("Error while processing answer! (User:" + user.getName() + ", Question:" + getQuestion(message.getContent()) + ")", e);
        }
        */

        logger.debug("Took " + (System.currentTimeMillis() - millis) + "ms");
    }

    private void processMessage(Message message) {
        long millis = System.currentTimeMillis();
        String content = message.getContent();
        String[] lines = content.split("\n");

        if (lines.length < 2) {
            return; // At least one answer
        }

        if (!lines[0].toLowerCase().startsWith(POLL_COMMAND)) {
            return;
        }
        logger.debug("The message is a valid poll message! (Question: " + getQuestion(message.getContent()) + ")");
        String question = getQuestion(lines[0]);

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }
            String[] components = line.split(" ", 2);
            String icon = components[0];

            Emoji emoji = null;

            // Handle custom emoji
            if (icon.startsWith("<")) {
                for(CustomEmoji customEmoji : discordApi.getCustomEmojis()) {
                    if(customEmoji.getMentionTag().equals(icon)) {
                        emoji = customEmoji;
                        break;
                    }
                }
                if(emoji == null) {
                    logger.warn("Illegal custom emoji! ID:"+ icon);
                    continue;
                }
            } else {
                emoji = ImplUnicodeEmoji.fromString(icon);
                if(emoji == null) {
                    logger.warn("Illegal unicode emoji! ID:"+ icon);
                    continue;
                }
            }

            message.addReaction(emoji);
        }
        pools.add(message.getId());
        logger.info("Added a new poll (Question: " + question + ")");
        logger.debug("Took " + (System.currentTimeMillis() - millis) + "ms");
    }

    private String getQuestion(String message) {
        return message.substring(POLL_COMMAND.length()).trim().split("\n")[0];
    }
}
