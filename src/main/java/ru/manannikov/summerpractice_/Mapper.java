package ru.manannikov.summerpractice_;

import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoSizes;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Mapper {
    private static final Logger LOG = LoggerFactory.getLogger(Mapper.class);

    public static FriendModel mapUserFullToFriendModel(GetResponse userFull, List<Photo> vkWallPhotos) {

        List<PhotoModel> modelWallPhotos = vkWallPhotos.stream().map(Mapper::mapVkPhotosToPhotoModel).toList();

        Map<YearMonth, Integer> wallPhotosNumberByYearMonth = Util.groupFriendPhotosByYearMonth(modelWallPhotos);

        return new FriendModel(
                userFull.getScreenName(),
                userFull.getId(),

                userFull.getFirstName(),
                userFull.getLastName(),

                new Image(userFull.getPhoto50().toString()),

                modelWallPhotos,

                wallPhotosNumberByYearMonth,

                Util.flattenPhotosNumberByYearMonthMapToMax(wallPhotosNumberByYearMonth)
        );
    }

    public static LocalDateTime mapUnixTimeToLocalDateTime(Long unixTime) {
        Instant instant = Instant.ofEpochSecond(unixTime);
        return LocalDateTime.ofInstant(instant, ZoneId.of("Europe/Moscow"));
    }
    // Получаю фотографии друга
    public static PhotoModel mapVkPhotosToPhotoModel(Photo vkPhoto) {
        PhotoSizes photoSize = vkPhoto.getSizes().get(2);

        double rw = 0.0, rh = 0.0;
        if (photoSize.getHeight() > photoSize.getWidth()) {
            rw = Util.PHOTO_SIZE;
        } else {
            rh = Util.PHOTO_SIZE;
        }

        return new PhotoModel(
                // Получаю изображение размера "x"
                Long.valueOf(vkPhoto.getId()),

                new Image(photoSize.getUrl().toString(), rw, rh, true, false, true),

                mapUnixTimeToLocalDateTime(
                        Long.valueOf(vkPhoto.getDate())
                )
        );
    }

    private static String mapMonthToString(Month month) {
        return month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
    }

    public static String mapMaxPhotosByYearMonthToString(Map.Entry<YearMonth, Integer> maxEntry) {
        YearMonth yearMonth = maxEntry.getKey();
        String month = mapMonthToString(yearMonth.getMonth());
        return String.format("в %s месяце %d года", month.substring(0, month.length() - 1).concat("е"), yearMonth.getYear());
    }

    public static String mapLocalDateTimeToString(LocalDateTime publicationDate) {
        return String.format("%td %<tB %<tY года в %<tH часов %<tM минут.", publicationDate);
    }

    public static ObservableList< XYChart.Data<String, Number> > mapYearMonthIntegerMapToChartData(Map<YearMonth, Integer> maxFriendsPhotosNumberByYearMonth) {
        List< XYChart.Data<String, Number> > list = maxFriendsPhotosNumberByYearMonth.entrySet().stream()
                .map( entry -> {
                    return new XYChart.Data<>(entry.getKey().toString(), (Number) entry.getValue());
                })
        .toList();

        return FXCollections.observableList(list);
    }
}
