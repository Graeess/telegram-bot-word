package org.example.telegrambotword.Service;

import lombok.Data;
import org.example.telegrambotword.Repository.UserRepository;
import org.example.telegrambotword.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
@Service
@Data
public class UserService {

    private final UserRepository userRepository;
    private final ConcurrentHashMap<String, String> userStates = new ConcurrentHashMap<>();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public Optional<User> findUserByChatId(String chatId) {
        return userRepository.findByChatId(chatId);
    }

    public void updateUserGender(String chatId, String gender) {
        findUserByChatId(chatId).ifPresent(user -> {
            user.setGender(gender);
            saveUser(user);
        });
    }

    public String getUserState(String chatId) {
        return userStates.getOrDefault(chatId, "START");
    }

    public void resetUserState(String chatId) {
        userStates.put(chatId, "START");
    }

    public void updateUserName(String chatId, String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите ФИО полностью: Имя Фамилия или Имя Фамилия Отчество");
        }

        String firstName = parts[0];
        String lastName = parts[1];
        String middleName = parts.length > 2 ? String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)) : null;

        findUserByChatId(chatId).ifPresentOrElse(user -> {
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setMiddleName(middleName);
            saveUser(user);
        }, () -> {
            User newUser = new User();
            newUser.setChatId(chatId);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setMiddleName(middleName);
            saveUser(newUser);


        });

    }

    public void updateUserState(String chatId, String state) {
        userStates.put(chatId, state);
    }

    public void updateUserBirthday(String chatId, String text) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate birthDate = LocalDate.parse(text, formatter);

            findUserByChatId(chatId).ifPresent(user -> {
                user.setBirthDate(birthDate);
                saveUser(user);
            });

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ошибка: Введите дату в формате ДД.ММ.ГГГГ");
        }
    }

    public void saveUserPhoto(String chatId, String fileId) {
        findUserByChatId(chatId).ifPresent(user -> {
            user.setPhotoId(fileId);
            saveUser(user);
        });
    }

    public String getUserPhoto(String chatId) {
        return findUserByChatId(chatId)
                .map(User::getPhotoId)
                .orElse(null);

    }

}
