package org.example.telegrambotword;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name ="users")

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String chatId;
    private String firstName;
    private String lastName;
    private String middleName;


    private LocalDate birthDate;
    private String gender;
    private String photoUrl;

    private String photoId;


}
