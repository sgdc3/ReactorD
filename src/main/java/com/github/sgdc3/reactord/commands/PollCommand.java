package com.github.sgdc3.reactord.commands;

import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.entities.channels.Channel;
import de.btobastian.javacord.entities.channels.ServerTextChannel;
import de.btobastian.javacord.entities.channels.TextChannel;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.emoji.CustomEmoji;
import de.btobastian.javacord.entities.message.emoji.Emoji;
import de.btobastian.javacord.entities.message.emoji.impl.ImplUnicodeEmoji;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@NoArgsConstructor
public class PollCommand implements CommandExecutor {

    @SuppressWarnings("unused")
    @Command(aliases = {"!sondaggio", "!poll"},
            description = "Create a new poll!",
            usage = "!poll [Question|Icon1 Option1;Icon2 Option2;Icon3 Option3]",
            async = true,
            privateMessages = false)
    public String onCommand(DiscordApi discordApi, ServerTextChannel serverChannel, Message message, String[] args) {
        // Discard private channel types
        if (serverChannel == null) {
            log.debug("Received a '!poll' command from a private channel (user: " + message.getAuthor().getName() + "), ignoring...");
            return null;
        }

        long millis = System.currentTimeMillis();
        log.debug("Received a '!poll' command from " + serverChannel.getServer().getName() + "@" +  message.getAuthor().getName() + ": " + message.getContent());

        // Delete the command
        message.delete().thenAccept(Void -> {
            log.debug("Deleted " + message.getAuthor().getName() + "'s sent command, took " + (System.currentTimeMillis() - millis) + "ms");
        });

        if (args == null || args.length < 2) {
            return "Wrong command usage! Minimum 2 args.";
        }
        //String[] arguments = Arrays.copyOfRange(args, 1, args.length);
        String rawArguments = String.join(" ", args);
        String[] arguments = rawArguments.split("\\|");
        if (arguments.length != 2) {
            return "Wrong command usage! Format.";
        }

        String question = arguments[0];
        String rawAnswers = arguments[1];
        String[] answers = rawAnswers.split(";");
        if (answers.length < 2) {
            return "You have to specify at least 2 answers!";
        }

        Map<Emoji, String> parsedAnswers = new LinkedHashMap<>();
        for (String answer : answers) {
            String[] components = answer.split(" ", 2);

            // No icon
            if (components.length < 2) {
                return "You need to define a valid emoji for the answer!";
            }
            String text = components[1];

            // Parse icon
            String icon = components[0];
            Emoji emoji = null;

            // Handle custom emoji
            if (icon.startsWith("<")) {
                for (CustomEmoji customEmoji : discordApi.getCustomEmojis()) {
                    if (customEmoji.getMentionTag().equals(icon)) {
                        emoji = customEmoji;
                        break;
                    }
                }
                if (emoji == null) {
                    log.warn("Illegal custom emoji! ID:" + icon);
                }
            } else {
                emoji = ImplUnicodeEmoji.fromString(icon);
            }

            if (emoji == null) {
                return "Unable to parse the emoji, try again!";
            }

            for (Map.Entry<Emoji, String> currentAnswer : parsedAnswers.entrySet()) {
                if (currentAnswer.getKey().equals(emoji) || currentAnswer.getValue().equals(text)) {
                    return "Duplicated answer or icon! Retry...";
                }
            }

            parsedAnswers.put(emoji, text);
        }

        StringBuilder response = new StringBuilder();
        response.append(".\n\nDomanda: ").append(question).append("\n");
        for (Map.Entry<Emoji, String> answer : parsedAnswers.entrySet()) {
            response.append("\n")
                    .append(answer.getKey().getMentionTag())
                    .append(": ").append(answer.getValue());
        }

        long sendMillis = System.currentTimeMillis();
        serverChannel.sendMessage(response.toString()).thenAccept(pollMessage -> {
            log.debug("Sent response to " + message.getAuthor().getName() + "'s command, took " + (System.currentTimeMillis() - sendMillis) + "ms");
            for (Emoji emoji : parsedAnswers.keySet()) {
                // One at time...
                long reactionMillis = System.currentTimeMillis();
                pollMessage.addReaction(emoji).thenAccept(Void -> {
                    log.debug("Added reaction to the response, original command by " + message.getAuthor().getName() + ", took " + (System.currentTimeMillis() - reactionMillis) + "ms");
                });
            }
        });

        log.info("Created new poll, author: " + message.getAuthor().getName() + ", server: " + serverChannel.getServer().getName() + ", channel: " + serverChannel.getName() + ", question: '" + question + "', took " + (System.currentTimeMillis() - millis) + "ms");
        return null;
    }
}
