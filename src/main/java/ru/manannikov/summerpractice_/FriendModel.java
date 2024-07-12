package ru.manannikov.summerpractice_;

import javafx.scene.image.Image;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

// Ключ -> идентификатор в хеш-таблице
public record FriendModel(
    String screenName,
    Long friendId,

    String firstName,
    String lastName,

    Image profilePhoto,

    List<PhotoModel> wallPhotos,
    // Имеет смысл хранить, но получение этого объекта лучше вынести в Util.
    Map<YearMonth, Integer> wallPhotosNumberByYearMonthMap,

    Map.Entry<YearMonth, Integer> maxPhotosNumberByYearMonth
)
{}
