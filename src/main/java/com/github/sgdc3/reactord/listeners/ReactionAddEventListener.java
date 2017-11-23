package com.github.sgdc3.reactord.listeners;

import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Reaction;
import de.btobastian.javacord.entities.message.emoji.Emoji;
import de.btobastian.javacord.events.message.reaction.ReactionAddEvent;
import de.btobastian.javacord.listeners.message.reaction.ReactionAddListener;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@NoArgsConstructor
public class ReactionAddEventListener implements ReactionAddListener {

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        final User user = event.getUser();
        // Skip self
        if (user.isYourself()) {
            return;
        }

        log.debug("Received a new reaction from user " + user.getName());
        long millis = System.currentTimeMillis();
        event.requestMessage().thenAccept(message -> {
            log.debug("Cached message data... took " + (System.currentTimeMillis() - millis) + "ms");

            if (!event.getReaction().isPresent()) {
                log.warn("Message wasn't cached!");
                return;
            }
            Reaction reaction = event.getReaction().get();

            // Only messages sent by the bot
            if (!message.getAuthor().isYourself()) {
                return;
            }

            log.debug("Processing reaction. (User:" + user.getName() + ", Message:" + message.getIdAsString() + ")");
            Emoji emoji = reaction.getEmoji();

            List<Reaction> reactions = message.getReactions();
            for (Reaction currentReaction : reactions) {
                Emoji currentEmoji = currentReaction.getEmoji();
                if (emoji.equals(currentEmoji)) {
                    continue;
                }
                currentReaction.removeUser(user);
            }
        });
    }
}
