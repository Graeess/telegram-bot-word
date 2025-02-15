package org.example.telegrambotword.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegrambotword.CustomTelegramBot;
import org.example.telegrambotword.Repository.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final CustomTelegramBot bot;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public byte[] downloadFile(String fileId) {
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            File file = bot.execute(getFile);


            String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();
            log.info("Скачивание файла по URL: {}", fileUrl);

            try (InputStream in = new URL(fileUrl).openStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                return out.toByteArray();
            }
        } catch (Exception e) {
            log.error("Ошибка при скачивании файла", e);
            return new byte[0];
        }
    }
}
