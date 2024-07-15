package ru.manannikov.summerpractice_;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

public class Util {

    private static final String parseExceptionMessage = "В сериализованном объекте \"%s\" отсутствуют необходимые для десериализации поля";
    public static final String INIT_WINDOW_TITLE = "ЛГТУ АСУ Летняя практика выполнил Мананников А. О. ПИ-22-2";
    public static final String INIT_FILE_NAME  = "без_названия";

    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setPrettyPrinting()

        .registerTypeAdapter(
            LocalDateTime.class,

            new JsonSerializer<LocalDateTime>() {
                @Override
                public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.toString());
                }
            }
        )

        .registerTypeAdapter(
            LocalDateTime.class,

            new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    return LocalDateTime.parse(json.getAsString());
                }
            }
        )

        .registerTypeAdapter(
            YearMonth.class,

            new JsonSerializer<YearMonth>() {
                @Override
                public JsonElement serialize(YearMonth src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.toString());
                }
            }
        )

        .registerTypeAdapter(
            YearMonth.class,

            new JsonDeserializer<YearMonth>() {
                @Override
                public YearMonth deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    return YearMonth.parse(json.getAsString());
                }
            }
        )

        .registerTypeAdapter(
            Image.class,

            new JsonSerializer<Image>() {
                @Override
                public JsonElement serialize(Image src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonObject imageJson = new JsonObject();

                    imageJson.addProperty("url", src.getUrl());
                    imageJson.addProperty("width", src.getWidth());
                    imageJson.addProperty("height", src.getHeight());

                    return imageJson;
                }
            }
        )

        .registerTypeAdapter(
            Image.class,

            new JsonDeserializer<Image>() {
                @Override
                public Image deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

                    JsonObject imageJson = json.getAsJsonObject();

                    double
                        w = imageJson.get("width").getAsDouble(),
                        h = imageJson.get("height").getAsDouble();
                    double rw = 0.0, rh = 0.0;
                    if (h > w) {
                        rw = Util.PHOTO_SIZE;
                    } else {
                        rh = Util.PHOTO_SIZE;
                    }

                    return new Image(imageJson.get("url").getAsString(), rw, rh, true, false, true);
                }
            }
        )

        .registerTypeAdapter(
            PhotoModel.class,

            new JsonDeserializer<PhotoModel>() {
                @Override
                public PhotoModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

                    JsonObject photoModelJson = json.getAsJsonObject();

                    JsonElement photoIdJson = photoModelJson.get("photo_id");
                    JsonElement photoJson = photoModelJson.get("photo");
                    JsonElement publicationDateJson = photoModelJson.get("publication_date");

                    if (
                        photoIdJson == null ||
                        photoJson == null ||
                        publicationDateJson == null
                    ) {
                        throw new JsonParseException(
                            String.format(parseExceptionMessage, typeOfT.getTypeName())
                        );
                    }

                    return new PhotoModel(
                        photoIdJson.getAsLong(),
                        context.deserialize(photoJson, Image.class),
                        context.deserialize(publicationDateJson, LocalDateTime.class)
                    );
                }
            }
        )

        .registerTypeAdapter(
            FriendModel.class,

            new JsonSerializer<FriendModel>() {
                @Override
                public JsonElement serialize(FriendModel src, Type typeOfSrc, JsonSerializationContext context) {

                    JsonObject friendJson = new JsonObject();

                    friendJson.addProperty("screen_name", src.screenName());
                    friendJson.addProperty("id", src.friendId());
                    friendJson.addProperty("first_name", src.firstName());
                    friendJson.addProperty("last_name", src.lastName());

                    friendJson.add("profile_photo", context.serialize(src.profilePhoto()));

                    friendJson.add("wall_photos", context.serialize(src.wallPhotos()));

                    friendJson.add("wall_photos_number_by_year_month", context.serialize(src.wallPhotosNumberByYearMonthMap()));

                    return friendJson;
                }
            }
        )

        .registerTypeAdapter(
            FriendModel.class,

            new JsonDeserializer<FriendModel>() {
                @Override
                public FriendModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

                    JsonObject friendJson = json.getAsJsonObject();

                    JsonElement screenNameJson = friendJson.get("screen_name");
                    JsonElement friendIdJson = friendJson.get("id");
                    JsonElement firstNameJson = friendJson.get("first_name");
                    JsonElement lastNameJson = friendJson.get("last_name");

                    JsonElement profilePhotoJson = friendJson.get("profile_photo");

                    JsonElement wallPhotosJson = friendJson.get("wall_photos");
                    JsonElement maxPhotosNumberByYearMonthMapJson = friendJson.get("wall_photos_number_by_year_month");

                    if (
                        screenNameJson == null ||
                        friendIdJson == null ||
                        firstNameJson == null ||
                        lastNameJson == null ||

                        profilePhotoJson == null ||
                        wallPhotosJson == null ||
                        maxPhotosNumberByYearMonthMapJson == null
                    ) {
                        throw new JsonParseException(
                            String.format(parseExceptionMessage, typeOfT.getTypeName())
                        );
                    }

                    List<PhotoModel> wallPhotos = context.deserialize(
                        wallPhotosJson,
                        new TypeToken< List<PhotoModel> >(){}.getType()
                    );
                    Map<YearMonth, Integer> maxPhotosNumberByYearMonthMap = context.deserialize(
                        maxPhotosNumberByYearMonthMapJson,
                        new TypeToken< Map<YearMonth, Integer> >(){}.getType()
                    );

                    String
                        firstName = firstNameJson.getAsString(),
                        lastName = lastNameJson.getAsString();

                    if (
                        wallPhotos.isEmpty() ||
                        maxPhotosNumberByYearMonthMap.isEmpty()
                    ) {
                        throw new IllegalArgumentException(
                            String.format("Список фотографий пользователя %s %s не может быть пустым.", firstName, lastName)
                        );
                    }

                    Map.Entry<YearMonth, Integer> maxPhotosNumberByYearMonth = Util.flattenPhotosNumberByYearMonthMapToMax(maxPhotosNumberByYearMonthMap);

                    return new FriendModel(
                        screenNameJson.getAsString(),
                        friendIdJson.getAsLong(),

                        firstName, lastName,

                        context.deserialize(profilePhotoJson, Image.class),

                        wallPhotos, maxPhotosNumberByYearMonthMap,

                        maxPhotosNumberByYearMonth
                    );
                }
            }
        )

        .registerTypeAdapter(
            TargetUserModel.class,

            new JsonDeserializer<TargetUserModel>() {
                @Override
                public TargetUserModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

                    JsonObject modelJson = json.getAsJsonObject();

                    JsonElement idJson = modelJson.get("id");
                    JsonElement firstNameJson = modelJson.get("first_name");
                    JsonElement lastNameJson = modelJson.get("last_name");

                    JsonElement friendIdsJson = modelJson.get("friend_ids");
                    JsonElement friendProfilesJson = modelJson.get("friend_profiles");

                    if (
                        idJson == null ||
                        firstNameJson == null ||
                        lastNameJson == null ||

                        friendIdsJson == null ||
                        friendProfilesJson == null
                    ) {
                        throw new JsonParseException(
                            String.format(parseExceptionMessage, typeOfT.getTypeName())
                        );
                    }

                    List<Long> friendIds = context.deserialize(
                        friendIdsJson,
                        new TypeToken< List<Long> >(){}.getType()
                    );
                    Map<Long, FriendModel> friendProfiles = context.deserialize(
                        friendProfilesJson,
                        new TypeToken< Map<Long, FriendModel> >(){}.getType()
                    );

                    String
                        firstName = firstNameJson.getAsString(),
                        lastName = lastNameJson.getAsString();

                    if (
                        friendIds.isEmpty() ||
                        friendProfiles.isEmpty()
                    ) {
                        throw new IllegalArgumentException(
                            String.format("Список фотографий пользователя %s %s не может быть пустым.", firstName, lastName)
                        );
                    }

                    return new TargetUserModel(
                        idJson.getAsLong(),
                        firstName,
                        lastName,

                        friendIds,
                        friendProfiles
                    );
                }
            }
        )

        .create();

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

    public static void serializeModel(final TargetUserModel model, final File file)
        throws IOException
    {
        // Нужно чтобы закрыть writer в любом из случаев
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(model, writer);
        } catch (IOException error) {
            throw new IOException(error);
        }
    }

    public static Optional<TargetUserModel> deserializeModel(final File file)
        throws IOException, JsonParseException, IllegalArgumentException
    {
        TargetUserModel model = null;
        try (Reader reader = new FileReader(file)) {
            model = GSON.fromJson(reader, TargetUserModel.class);
        } catch (IOException error) {
            throw new IOException(error);
        }
        return Optional.ofNullable(model);
    }

    public static void exportBarChart(File file, String ext, Image img)
        throws IOException
    {
        String fileName = file.getName().toLowerCase();
        switch (ext) {
            case "png":
                if (!fileName.endsWith("png"))
                    file = new File(file.getParentFile(), fileName.concat(".png"));
                break;
            case "jpg":
                if (!fileName.endsWith("jpeg") && !fileName.endsWith("jpg"))
                    file = new File(file.getParentFile(), fileName.concat(".jpg"));
                break;
        }

        BufferedImage bImg = SwingFXUtils.fromFXImage(img, null);

        ImageIO.write(bImg, ext, file);
    }
}
