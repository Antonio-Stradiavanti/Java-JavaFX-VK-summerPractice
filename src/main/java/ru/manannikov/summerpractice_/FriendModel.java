package ru.manannikov.summerpractice_;

import java.net.URI;

public record FriendModel(
        String screenName,
        String firstName,
        String lastName,
        URI photo
)
{}
