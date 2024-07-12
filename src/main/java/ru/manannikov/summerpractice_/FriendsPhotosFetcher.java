package ru.manannikov.summerpractice_;

import com.vk.api.sdk.client.Lang;
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

// Все запросы отправляю с помощью сервисного ключа, то есть через ServiceActor.
public class FriendsPhotosFetcher extends Task<TargetUserModel> {
    private static final Logger LOG = LoggerFactory.getLogger(FriendsPhotosFetcher.class);

    private final int MAX_REQUESTS_PER_SECOND = 5;
    private final int SLEEP_TIME_MS = 1000 / MAX_REQUESTS_PER_SECOND;

    // Для доступа к API
    private final VkApiClient vkApiClient;
    private final ServiceActor serviceActor;

    private final String screenName;
    private final TargetUserModel model;

    // Конструктор
    public FriendsPhotosFetcher(Integer appId, String serviceToken, String screenName) {
        TransportClient transportClient = HttpTransportClient.getInstance();
        vkApiClient = new VkApiClient(transportClient);
        serviceActor = new ServiceActor(appId, serviceToken);

        this.screenName = screenName;

        model = new TargetUserModel();
    }

    private void fetchUser() throws ApiException, ClientException {
        // Теперь get возвращает builder
        // GetResponse = User, Пользователь, мне нужен первый из списка пользователей.
       com.vk.api.sdk.objects.users.responses.GetResponse usersResponse = vkApiClient.users().get(serviceActor)
               .lang(Lang.RU)
               .userIds(screenName)
               .execute().getFirst();

        model.setId(usersResponse.getId());
        model.setFirstName(usersResponse.getFirstName());
        model.setLastName(usersResponse.getLastName());
    }

    private void fetchFriendIds() throws Exception {
        try {
            updateMessage("Получаем список ваших друзей ...");

            com.vk.api.sdk.objects.friends.responses.GetResponse friendIdsResponse = vkApiClient.friends().get(serviceActor)
                    .userId(model.getId())
                    // Пользователи отсортированы в алфавитном порядке
                    .order(GetOrder.NAME)
                    .execute();

            model.getFriendIds().addAll(friendIdsResponse.getItems());
        } catch (ApiExtendedException e) {
            throw new Exception("У вас закрытый профиль, невозможно получить фотографии ваших друзей.");
        }
    }

    private List<Photo> fetchFriendPhotos(Long userId) throws ApiException, ClientException {
        // rev -> антихронологический порядок -- самый первый элемент списка -- текущая аватарка, последняя опубликованная фотка.
        try {
            com.vk.api.sdk.objects.photos.responses.GetResponse photoResponse = vkApiClient.photos().get(serviceActor)
                    .lang(Lang.RU)
                    .ownerId(userId)
                    .albumId("wall")
                    .rev(true)
                    .execute();

            return photoResponse.getItems();
        } catch (ApiExtendedException e) {
            LOG.error("Возникла ошибка \"{}\" -> не удалось получить фотки пользователя с id = {}, так как его профиль является закрытым и соответственно недоступен для запросов от имени приложения.", e, userId);
            return new ArrayList<>();
        }
    }

    // TODO, пофиксить метод
    private void fetchFriendProfiles() throws ApiException, ClientException {

        List<com.vk.api.sdk.objects.users.responses.GetResponse> friendProfilesResponse = vkApiClient.users().get(serviceActor)
                .lang(Lang.RU)
                .userIds(model.getFriendIds().stream().map(String::valueOf).toList())
                .fields(Fields.SCREEN_NAME, Fields.PHOTO_50)
                .execute();

        try {
            // Тут можно использовать только такой цикл.
            int friendsNumber = model.getFriendIds().size();
            for (int i = 0; i < friendsNumber; i++) {
                // Надо получить фотки
                List<Photo> friendWallPhotos = fetchFriendPhotos(model.getFriendIds().get(i));
                // Если у друга нет фоток или у него закрытый профиль, то я его не добавляю в модель.
                if (!friendWallPhotos.isEmpty()) {
                    FriendModel friend = Mapper.mapUserFullToFriendModel(friendProfilesResponse.get(i), friendWallPhotos);
                    model.getFriendProfiles().put(
                            // Полученный от API список ключей позволяет выводить пользователей в алфавитном порядке их ФИ.
                            model.getFriendIds().get(i),
                            friend
                    );
                }

                updateMessage(String.format("Получаем профили ваших друзей и собираем информацию об опубликованных ими фотографиях ... получены данные %d из %d друзей.", i, friendsNumber));
                // !!! Несколько запросов, нужно ограничить количество запросов до 5 в секунду
                Thread.sleep(SLEEP_TIME_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    @Override
    protected TargetUserModel call() throws Exception {
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
        // Позвращаем полученную модель
        return model;
    }

}
