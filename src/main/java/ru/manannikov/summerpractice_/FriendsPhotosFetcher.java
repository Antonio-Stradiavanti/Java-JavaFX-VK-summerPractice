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
import com.vk.api.sdk.objects.users.UserMin;
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

// Все запросы отправляю с помощью сервисного ключа, то есть через ServiceActor.
public class FriendsPhotosFetcher extends Task<FetchedDataModel> {
    private static final Logger LOG = LoggerFactory.getLogger(FriendsPhotosFetcher.class);

    private final int MAX_REQUESTS_PER_SECOND = 5;
    private final int SLEEP_TIME_MS = 1000 / MAX_REQUESTS_PER_SECOND;

    // Для доступа к API
    private final VkApiClient vkApiClient;
    private final ServiceActor serviceActor;

    private final String screenName;
    private final FetchedDataModel model;

    // Конструктор
    public FriendsPhotosFetcher(Integer appId, String serviceToken, String screenName) {
        TransportClient transportClient = HttpTransportClient.getInstance();
        vkApiClient = new VkApiClient(transportClient);
        serviceActor = new ServiceActor(appId, serviceToken);

        this.screenName = screenName;
        model = new FetchedDataModel();
    }

    private void fetchUser() throws ApiException, ClientException {
        // Теперь get возвращает builder
        // GetResponse = User, Пользователь, мне нужен первый из списка пользователей.
       com.vk.api.sdk.objects.users.responses.GetResponse usersResponse = vkApiClient.users().get(serviceActor).userIds(screenName).execute().getFirst();

        UserMin user = new UserMin();
        user.setId(usersResponse.getId());
        user.setFirstName(usersResponse.getFirstName());
        user.setLastName(usersResponse.getLastName());

        model.setUser(user);
    }

    private void fetchFriendProfiles() throws ApiException, ClientException {
        updateMessage("Получаем список профилей ваших друзей ...");

        List<com.vk.api.sdk.objects.users.responses.GetResponse> friendProfilesResponse = vkApiClient.users().get(serviceActor)
            .userIds(model.getFriendIds().stream().map(String::valueOf).toList())
            .fields(Fields.SCREEN_NAME, Fields.PHOTO_200)
            .execute();

        for (int i = 0; i < model.getFriendIds().size(); i++) {
            model.getFriendsProfiles().put(
                    model.getFriendIds().get(i),
                    Mapper.mapUserFullToFriendModel(friendProfilesResponse.get(i))
            );
        }

    }

    private void fetchFriendIds() throws Exception {
        try {
            updateMessage("Получаем список ваших друзей ...");

            com.vk.api.sdk.objects.friends.responses.GetResponse friendsResponse = vkApiClient.friends().get(serviceActor).userId(model.getUser().getId()).order(GetOrder.NAME).execute();

            model.setFriendIds(friendsResponse.getItems());
        } catch (ApiExtendedException e) {
            throw new Exception("У вас закрытый профиль, невозможно получить фотографии ваших друзей.");
        }
    }

    private List<Photo> fetchFriendPhotos(Long userId) throws ApiException, ClientException {
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

    private void fetchFriendsPhotos() throws ApiException, ClientException {
        try {
            int friendIdsCount = model.getFriendIds().size();
            for (int i = 0; i < friendIdsCount; ++i) {
                Long friendId = model.getFriendIds().get(i);

                // Получаем фотографии друга
                List<Photo> friendPhotos = fetchFriendPhotos(friendId);

                model.getFriendsPhotos().put(friendId, friendPhotos);

                // Для каждого друга получаем информацию о количестве фотографий в определенный месяц/год
                // Если список фотографий был пустой, то пустым будет и соотв. словарь
                model.getFriendsPhotosNumberByYearMonth().put(friendId, createFriendPhotosNumberByYearMonth(friendPhotos));

                updateMessage(String.format("Получаем фотографии ваших друзей ... получены фотографии %d из %d друзей.", i, friendIdsCount));
                // !!! Несколько запросов, нужно ограничить количество запросов до 5 в секунду
                Thread.sleep(SLEEP_TIME_MS);
            }
        } catch (InterruptedException e) {
//            LOG.info("Завершаю выполнение потока ... {}", Thread.currentThread());
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected FetchedDataModel call() throws Exception {
        // например: 364320788 -- я, или 820016119 -- бро
        updateMessage("Выполняем запрос ...");
        if (screenName.isEmpty()) {
            throw new IllegalArgumentException("Передана недопустимая ссылка");
        }
        // Получаем профиль пользователя, о фотографиях друзей которого будем собирать информацию.
        fetchUser();
        // Получаем список идентификаторов его друзей.
        fetchFriendIds();
        // Получаем профили друзей
        fetchFriendProfiles();
        // Получаем фотографии друзей пользователя
        fetchFriendsPhotos();
        // Позвращаем полученную модель
        return model;
    }

}
