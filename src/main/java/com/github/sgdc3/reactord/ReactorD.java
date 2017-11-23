package com.github.sgdc3.reactord;

import com.github.sgdc3.reactord.commands.PollCommand;
import com.github.sgdc3.reactord.listeners.ReactionAddEventListener;
import de.btobastian.javacord.DiscordApi;
import de.btobastian.javacord.DiscordApiBuilder;
import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
public final class ReactorD {

    public ReactorD(String token) {
        log.info("Welcome to ReactorD, connecting with token = " + token);

        // Connect
        DiscordApi discordApi;
        try {
            long millis = System.currentTimeMillis();
            discordApi = new DiscordApiBuilder().setToken(token).login().get();
            log.info("Connected! Took " + (System.currentTimeMillis() - millis) + "ms");
            log.info("Server count: " + discordApi.getServers().size());
        } catch (InterruptedException | ExecutionException e) {
            log.error("A fatal error occurred.", e);
            return;
        }

        // Register commands
        CommandHandler commandHandler = new JavacordHandler(discordApi);
        commandHandler.registerCommand(new PollCommand());

        // Register listeners
        discordApi.addReactionAddListener(new ReactionAddEventListener());

        log.info("Ready!");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            log.error("Invalid arguments... Required arguments: Token");
            return;
        }
        new ReactorD(args[0]);
    }
}
