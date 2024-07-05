package ru.manannikov.summerpractice_;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ApiExtendedException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.friends.GetOrder;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.users.Fields;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsPhotosFetcher extends Task<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(FriendsPhotosFetcher.class);

    private final int MAX_REQUESTS_PER_SECOND = 5;
    private final int SLEEP_TIME_MS = 1000 / MAX_REQUESTS_PER_SECOND;

    // Для доступа к API
    private final VkApiClient vkApiClient;
    private final ServiceActor serviceActor;

    // Данные
    private String name = "";
    private List<Long> friendIds;
    private Map<Long, GetResponse> friendsProfiles = new HashMap<>();
    private Map<Long, List<Photo>> friendsPhotos = new HashMap<>();
    private Map<Long, Map<YearMonth, Integer>> friendsPhotosNumberByYearMonth = new HashMap<>();

    // Конструктор
    public FriendsPhotosFetcher(Integer appId, String serviceToken) {
        TransportClient transportClient = HttpTransportClient.getInstance();
        vkApiClient = new VkApiClient(transportClient);
        serviceActor = new ServiceActor(appId, serviceToken);
        updateMessage("Загрузите информацию о фотографиях своих друзей из файла, или отправьте запрос к vk API ");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Long> getFriendIds() {
        return friendIds;
    }

    public Map<Long, GetResponse> getFriendsProfiles() {
        return friendsProfiles;
    }

    public Map<Long, List<Photo>> getFriendsPhotos() {
        return friendsPhotos;
    }

    public Map<Long, Map<YearMonth, Integer>> getFriendsPhotosNumberByYearMonth() {
        return friendsPhotosNumberByYearMonth;
    }

    private Long fetchUserId(String name) throws ApiException, ClientException {
        // Теперь get возвращает builder
        // GetResponse = User, Пользователь, мне нужен первый из списка пользователей.
        List<com.vk.api.sdk.objects.users.responses.GetResponse> usersResponse = vkApiClient.users().get(serviceActor).userIds(name).execute();
        // Возвращаем id пользователя
        return usersResponse.getFirst().getId();
    }

    private List<com.vk.api.sdk.objects.users.responses.GetResponse> fetchUsers(List<String> ids) throws ApiException, ClientException {
        return vkApiClient.users().get(serviceActor).userIds(ids).fields(Fields.PHOTO_200).execute();
    }

    private List<Long> fetchFriendIds(Long userId) throws ApiException, ClientException {

        // Отправляю запросы через ServiceActor
        com.vk.api.sdk.objects.friends.responses.GetResponse friendsResponse = vkApiClient.friends().get(serviceActor).userId(userId).order(GetOrder.NAME).execute();

        return friendsResponse.getItems();
    }

    private List<Photo> fetchPhotos(Long userId) throws ApiException, ClientException {
        // rev -> антихронологический порядок -- самый первый элемент списка -- текущая аватарка, последняя опубликованная фотка.
        try {
            com.vk.api.sdk.objects.photos.responses.GetResponse photoResponse = vkApiClient.photos().get(serviceActor).ownerId(userId).albumId("wall").rev(true).execute();
            return photoResponse.getItems();
        } catch (ApiExtendedException e) {
            LOG.error("Возникла ошибка \"{}\" -> не удалось получить фотки пользователя с id = {}, так как его профиль является закрытым и соответственно недоступен для запросов от имени приложения.", e, userId);
            return new ArrayList<>();
        }
    }

    private LocalDateTime unixTimeToLocalDateTime(Long unixTime) {
        Instant instant = Instant.ofEpochSecond(unixTime);
        return LocalDateTime.ofInstant(instant, ZoneId.of("Europe/Moscow"));
    }

    private Map<YearMonth, Integer> createFriendPhotosNumberByYearMonth(List<Photo> friendPhotos) {
        Map<YearMonth, Integer> friendPhotosNumberByYearMonth = new HashMap<>();

        friendPhotos.forEach(photo -> {
            YearMonth yearMonth = YearMonth.from(
                    unixTimeToLocalDateTime(Long.valueOf(photo.getDate()))
            );
            friendPhotosNumberByYearMonth.merge(yearMonth, 1, Integer::sum);
        });

        return friendPhotosNumberByYearMonth;
    }

    // Рез может быть нулем.
    private YearMonth findMaxFriendPhotosNumberByYearMonth(Map<YearMonth, Integer> friendPhotosNumberByYearMonth) {
        YearMonth maxYearMonth = null;
        // Такого быть не может.
        int maxCount = 0;

        for (Map.Entry<YearMonth, Integer> entry : friendPhotosNumberByYearMonth.entrySet()) {
            YearMonth key = entry.getKey();
            Integer value = entry.getValue();

            if (value > maxCount) {
                maxCount = value;
                maxYearMonth = key;
            }
        }

        return maxYearMonth;
    }

    @Override
    protected Void call() {

        // например: 364320788 -- я, или 820016119 -- бро
        try {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Передана недопустимая ссылка");
            }
            Long userId = fetchUserId(name);
            // Перечень всех друзей пользователя.
            // По идентификаторам друзей можно получить всю информацию.
            // ! Один запрос
            friendIds = fetchFriendIds(userId);
            int friendIdsCount = friendIds.size();
            // Выведем перечень идентификаторов
            LOG.info("friendIds = {}", friendIds);

            // Перечень идентификаторов друзей
            // ! Один запрос
            List<com.vk.api.sdk.objects.users.responses.GetResponse> friendList = fetchUsers(friendIds.stream().map(String::valueOf).toList());

            // !!! Несколько запросов, нужно ограничить количество запросов до 5 в секунду
            for (int i = 0; i < friendIdsCount; ++i) {
                Long friendId = friendIds.get(i);

                friendsProfiles.put(friendId, friendList.get(i));

                List<Photo> friendPhotos = fetchPhotos(friendId);

                friendsPhotos.put(friendId, friendPhotos);

                friendsPhotosNumberByYearMonth.put(friendId, createFriendPhotosNumberByYearMonth(friendPhotos));

                updateMessage(String.format("Получаем фотографии ваших друзей ... получены фотографии %d из %d друзей.", i, friendIdsCount));
                Thread.sleep(SLEEP_TIME_MS);
            }

            LOG.info("friendsPhotosNumberByYearMonth = {}\nМесяц/год, в котором первый друг опубликовал больше всего фотографий -> {}",
                    friendsPhotosNumberByYearMonth,
                    findMaxFriendPhotosNumberByYearMonth(friendsPhotosNumberByYearMonth.get(friendIds.getFirst()))
            );
            // Логи я пишу для себя
        } catch (InterruptedException e) {
            LOG.info("Завершаю выполнение потока {}", Thread.currentThread());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("При выполнении запроса к vk API возникло исключение {}", e.toString());
        }
        return null;
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        updateMessage("Выполнение задачи отменено.");
    }

    @Override
    protected void failed() {
        super.failed();
        updateMessage("В ходе выполнения задачи возникли ошибки, см. логи :(");
    }

    @Override
    public void succeeded() {
        super.succeeded();
        updateMessage("Данные успешно получены от vk API");
    }

}
