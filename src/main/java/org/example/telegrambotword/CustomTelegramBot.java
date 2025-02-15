package org.example.telegrambotword;

import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;

public class CustomTelegramBot extends DefaultAbsSender {

    private final String botToken;

    public CustomTelegramBot(DefaultBotOptions options, String botToken) {
        super(options);
        this.botToken = botToken;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
