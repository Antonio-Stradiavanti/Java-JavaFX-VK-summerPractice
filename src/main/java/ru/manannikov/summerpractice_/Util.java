package ru.manannikov.summerpractice_;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Util {

    public static final double PHOTO_SIZE = 270.0;

    public static Map<YearMonth, Integer> groupFriendPhotosByYearMonth(List<PhotoModel> modelWallPhotos) {
        Map<YearMonth, Integer> wallPhotosNumberByYearMonth = new HashMap<>();

        modelWallPhotos.forEach(wallPhoto -> {
            wallPhotosNumberByYearMonth.merge(YearMonth.from(wallPhoto.publicationDate()), 1, Integer::sum);
        });

        return wallPhotosNumberByYearMonth;
    }

    public static Map.Entry<YearMonth, Integer> flattenPhotosNumberByYearMonthMapToMax(Map<YearMonth, Integer> photosGroupedByYearMonth) {
        return photosGroupedByYearMonth.entrySet().stream().max(
                Map.Entry.comparingByValue()
        ).orElseThrow();
    }

    public static Map<YearMonth, Integer> findMaxPhotosByYearMonthMap(Map<Long, FriendModel> friendProfiles) {
        // friendProfiles гарантированно содержит фотки
        Map<YearMonth, Integer> globalMaxFriendsPhotosByYearMonth = new TreeMap<>();
        for (Long friendId : friendProfiles.keySet()) {
            // Получаю хештаблицу состояющую из количеств обубликованных другом фотографий за определенный месяц/год
            // Свожу полученную хештаблицу к максимуму.
            Map.Entry<YearMonth, Integer> localMaxFriendsPhotosByYearMonth = friendProfiles.get(friendId).maxPhotosNumberByYearMonth();
            // Добавляю полученный максимум в ассоциативный массив.
            globalMaxFriendsPhotosByYearMonth.merge(
                    localMaxFriendsPhotosByYearMonth.getKey(),
                    localMaxFriendsPhotosByYearMonth.getValue(),
                    Integer::sum
            );
        }
        return globalMaxFriendsPhotosByYearMonth;
    }
}
