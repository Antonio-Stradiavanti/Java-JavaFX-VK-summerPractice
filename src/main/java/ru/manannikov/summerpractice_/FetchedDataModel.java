package ru.manannikov.summerpractice_;

import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.users.UserMin;
import javafx.scene.chart.XYChart;

import java.time.YearMonth;
import java.util.*;

public class FetchedDataModel {
    private UserMin user;
    private List<Long> friendIds;
    
    private final Map<Long, FriendModel> friendsProfiles = new HashMap<>();
    private final Map<Long, List<Photo>> friendsPhotos = new HashMap<>();
    private final Map<Long, Map<YearMonth, Integer>> friendsPhotosNumberByYearMonth = new HashMap<>();

    public void setFriendIds(List<Long> friendIds) {
        this.friendIds = friendIds;
    }

    public UserMin getUser() {
        return user;
    }
    
    public void setUser(UserMin user) {
        this.user = user;    
    }

    public List<Long> getFriendIds() {
        return friendIds;
    }

    public Map<Long, FriendModel> getFriendsProfiles() {
        return friendsProfiles;
    }

    public Map<Long, List<Photo>> getFriendsPhotos() {
        return friendsPhotos;
    }

    public Map<Long, Map<YearMonth, Integer>> getFriendsPhotosNumberByYearMonth() {
        return friendsPhotosNumberByYearMonth;
    }

    // Бизнес-логика
    // Надо сделать 2 метода : поиск максимума для локальных графиков & поиск ассоциативного массива максимумов для глобального графика, через метод merge.
    public Map<YearMonth, Integer> findMaxFriendsPhotosNumberByYearMonth() {
        Map<YearMonth, Integer> maxFriendsPhotosNumberByYearMonth = new TreeMap<>();
        // Надо чтобы значения по оси категорий шли в порядке возрастания ключей
        for (Long friendId : friendIds) {

            // Получаю хештаблицу состояющую из количеств обубликованных другом фотографий за определенный месяц/год
            var friendPhotosNumberByYearMonth = friendsPhotosNumberByYearMonth.get(friendId);

            // Если она пустая, то дальше делать нечего.
            if (friendPhotosNumberByYearMonth.isEmpty()) continue;

            // Свожу полученную хештаблицу к максимуму.
            Map.Entry<YearMonth, Integer> maxFriendPhotosNumberByYearMonth = friendPhotosNumberByYearMonth.entrySet().stream().max(
                    Map.Entry.comparingByValue()
            ).orElseThrow();

            // Добавляю полученный максимум в ассоциативный массив.
            maxFriendsPhotosNumberByYearMonth.merge(
                    maxFriendPhotosNumberByYearMonth.getKey(),
                    maxFriendPhotosNumberByYearMonth.getValue(),
                    Integer::sum
            );

        }

        return maxFriendsPhotosNumberByYearMonth;
    }
}
