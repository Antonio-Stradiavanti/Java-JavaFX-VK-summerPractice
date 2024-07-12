package ru.manannikov.summerpractice_;

import javafx.scene.image.Image;

import java.time.LocalDateTime;

public record PhotoModel(
    Long photoId,
    Image photo,
    LocalDateTime publicationDate
)
{}