package org.example.telegrambotword.Configuration;

import org.example.telegrambotword.CustomTelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Configuration
public class TelegramBotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Bean(name = "customTelegramBot")
    public CustomTelegramBot telegramBot() {
        DefaultBotOptions options = new DefaultBotOptions();
        return new CustomTelegramBot(options, botToken);
    }
}

