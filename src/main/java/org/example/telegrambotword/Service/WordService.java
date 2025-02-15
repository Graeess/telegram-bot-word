package org.example.telegrambotword.Service;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.example.telegrambotword.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class WordService {

    private static final Logger log = LoggerFactory.getLogger(WordService.class);
    private static final String DOCUMENTS_DIR = "generated_docs/";

    public SendDocument generateDocument(User user) {
        String fileName = "document_" + user.getChatId() + ".docx";
        Path filePath = Path.of(DOCUMENTS_DIR, fileName);

        try {
            Files.createDirectories(filePath.getParent());
            createWordDocument(user, filePath.toString());
        } catch (IOException e) {
            log.error("Ошибка при создании директории для документов", e);
            return null;
        }

        File file = filePath.toFile();
        if (!file.exists()) {
            log.error("Ошибка: файл {} не найден!", filePath);
            return null;
        }

        return new SendDocument(user.getChatId(), new InputFile(file));
    }

    private void createWordDocument(User user, String filePath) {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(filePath)) {

            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText("ФИО: " + user.getLastName() + " " + user.getFirstName() +
                    (user.getMiddleName() != null ? " " + user.getMiddleName() : ""));
            run.addBreak();
            run.setText("Дата рождения: " + (user.getBirthDate() != null ? user.getBirthDate().toString() : "Не указано"));
            run.addBreak();
            run.setText("Пол: " + (user.getGender() != null ? user.getGender() : "Не указано"));
            run.addBreak();
            run.addBreak();


            if (user.getPhotoId() != null && !user.getPhotoId().isEmpty()) {
                addImageToDocument(document, user.getPhotoId());
            }

            document.write(out);
            log.info("Документ успешно создан: {}", filePath);

        } catch (Exception e) {
            log.error("Ошибка при создании документа", e);
        }
    }

    private void addImageToDocument(XWPFDocument document, String photoPath) {
        File photoFile = new File(photoPath);
        if (!photoFile.exists()) {
            log.warn("Файл фото не найден: {}", photoPath);
            return;
        }

        try (FileInputStream fis = new FileInputStream(photoFile)) {
            XWPFParagraph imgParagraph = document.createParagraph();
            XWPFRun imgRun = imgParagraph.createRun();
            imgRun.addPicture(fis, XWPFDocument.PICTURE_TYPE_JPEG, photoFile.getName(),
                    Units.toEMU(200), Units.toEMU(200));
            log.info("Фото добавлено в документ.");
        } catch (Exception e) {
            log.error("Ошибка при добавлении фото в документ", e);
        }
    }
}
