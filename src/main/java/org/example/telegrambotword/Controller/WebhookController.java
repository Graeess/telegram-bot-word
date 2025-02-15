package org.example.telegrambotword.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.telegrambotword.TelegramBot;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final TelegramBot telegramBot;

    public WebhookController(@Lazy TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @PostMapping
    public ResponseEntity<BotApiMethod<?>> onUpdateReceived(@RequestBody Update update) {
        try {
            if (log.isInfoEnabled()) {
                log.info("Получен запрос от Telegram: {}", update);
            }

            BotApiMethod<?> response = telegramBot.onWebhookUpdateReceived(update);

            if (response == null) {
                log.warn("Бот не сгенерировал ответ! Отправляем заглушку.");
                return ResponseEntity.ok(new SendMessage(update.getMessage().getChatId().toString(), "Что-то пошло не так, попробуйте ещё раз."));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при обработке вебхука", e);
            return ResponseEntity.ok().build();
        }
    }
}
