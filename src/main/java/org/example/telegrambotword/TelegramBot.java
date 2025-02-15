package org.example.telegrambotword;


import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegrambotword.Repository.FileService;
import org.example.telegrambotword.Service.UserService;
import org.example.telegrambotword.Service.WordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@RequiredArgsConstructor
@Component("telegramBotComponent")
public class TelegramBot extends TelegramWebhookBot {

    private final UserService userService;
    private final WordService wordService;
    private final FileService fileService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.webhook-path}")
    private String webhookPath;

    @PostConstruct
    public void initialize() {
        try {
            this.execute(SetWebhook.builder().url(webhookPath).build());
            log.info("Webhook установлен на URL: {}", webhookPath);

            List<BotCommand> commands = List.of(
                    new BotCommand("/start", "Запустить бота"),
                    new BotCommand("/help", "Помощь"),
                    new BotCommand("/generate", "Создать документ")
            );
            this.execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка при настройке бота", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotPath() {
        return webhookPath;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return (BotApiMethod<?>) handleTextMessage(update);
        } else if (update.hasCallbackQuery()) {
            return handleCallbackQuery(update);
        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            return handlePhotoMessage(update);
        }
        return null;
    }

    private Object handleTextMessage(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText();

        return switch (text) {
            case "/start" -> {
                userService.resetUserState(chatId);
                yield sendWelcomeMessage(chatId);
            }
            case "/help" -> sendHelpMessage(chatId);
            case "/generate" -> handleGenerateCommand(chatId);
            default -> handleUserInput(chatId, text);
        };
    }

    private BotApiMethod<?> handleCallbackQuery(Update update) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        String callbackData = update.getCallbackQuery().getData();

        if ("male".equals(callbackData) || "female".equals(callbackData)) {
            String gender = "male".equals(callbackData) ? "Мужской" : "Женский";
            userService.updateUserGender(chatId, gender);
            userService.updateUserState(chatId, "WAITING_PHOTO");
            return new SendMessage(chatId, "Вы выбрали: " + gender + ". Теперь отправьте фото.");
        }
        return new SendMessage(chatId, "Некорректный выбор.");
    }

    private BotApiMethod<?> handlePhotoMessage(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if ("WAITING_PHOTO".equals(userService.getUserState(chatId))) {
            String fileId = update.getMessage().getPhoto().get(update.getMessage().getPhoto().size() - 1).getFileId();
            try {
                byte[] imageData = fileService.downloadFile(fileId);
                java.nio.file.Path imageDir = java.nio.file.Paths.get("images");


                if (!java.nio.file.Files.exists(imageDir)) {
                    java.nio.file.Files.createDirectories(imageDir);
                }

                java.nio.file.Path imagePath = imageDir.resolve(fileId + ".jpg");
                java.nio.file.Files.write(imagePath, imageData);
                userService.saveUserPhoto(chatId, imagePath.toString());
                userService.updateUserState(chatId, "COMPLETED");
                return new SendMessage(chatId, "Фото загружено! Теперь вы можете использовать команду /generate для создания документа.");
            } catch (Exception e) {
                log.error("Ошибка при обработке фото", e);
                return new SendMessage(chatId, "Ошибка при обработке фото.");
            }
        }
        return new SendMessage(chatId, "Сначала завершите предыдущие шаги.");
    }

    private BotApiMethod<?> handleGenerateCommand(String chatId) {
        Optional<User> userOpt = userService.findUserByChatId(chatId);
        if (userOpt.isEmpty()) {
            return new SendMessage(chatId, "Ошибка: данные не найдены.");
        }
        User user = userOpt.get();
        SendDocument document = wordService.generateDocument(user);
        try {
            execute(document);
            return new SendMessage(chatId, "Документ отправлен!");
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке документа", e);
            return new SendMessage(chatId, "Ошибка при создании документа.");
        }
    }

    private SendMessage sendWelcomeMessage(String chatId) {
        userService.updateUserState(chatId, "WAITING_FIO");
        return new SendMessage(chatId, "Введите ваше ФИО:");
    }

    private SendMessage sendHelpMessage(String chatId) {
        return new SendMessage(chatId, "Команды: /start, /help, /generate");
    }

    private BotApiMethod<?> handleUserInput(String chatId, String text) {
        String userState = userService.getUserState(chatId);

        switch (userState) {
            case "WAITING_FIO":
                userService.updateUserName(chatId, text);
                userService.updateUserState(chatId, "WAITING_BIRTHDAY");
                return new SendMessage(chatId, "Спасибо! Теперь введите вашу дату рождения (в формате ДД.ММ.ГГГГ):");

            case "WAITING_BIRTHDAY":
                if (isValidDate(text)) {
                    userService.updateUserBirthday(chatId, text);
                    userService.updateUserState(chatId, "WAITING_GENDER");
                    SendMessage message = new SendMessage(chatId, "Дата рождения сохранена! Теперь выберите ваш пол:");
                    message.setReplyMarkup(createGenderKeyboard());
                    return message;
                } else {
                    return new SendMessage(chatId, "Некорректный формат даты. Пожалуйста, введите дату в формате ДД.ММ.ГГГГ.");
                }

            case "WAITING_GENDER":
                return new SendMessage(chatId, "Пожалуйста, выберите пол с помощью кнопок ниже:");

            case "WAITING_PHOTO":
                return new SendMessage(chatId, "Отправьте фото, чтобы завершить регистрацию.");

            case "COMPLETED":
                return new SendMessage(chatId, "Вы уже завершили регистрацию. Используйте /generate для создания документа.");

            default:
                return new SendMessage(chatId, "Я не понимаю ваш ввод. Используйте /help для списка команд.");
        }
    }

    private InlineKeyboardMarkup createGenderKeyboard() {
        InlineKeyboardButton maleButton = new InlineKeyboardButton("Мужской");
        maleButton.setCallbackData("male");

        InlineKeyboardButton femaleButton = new InlineKeyboardButton("Женский");
        femaleButton.setCallbackData("female");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(List.of(maleButton, femaleButton));  // Два ряда кнопок

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }


    private boolean isValidDate(String date) {
        String regex = "^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[0-2])\\.(\\d{4})$";
        return date.matches(regex);
    }
}