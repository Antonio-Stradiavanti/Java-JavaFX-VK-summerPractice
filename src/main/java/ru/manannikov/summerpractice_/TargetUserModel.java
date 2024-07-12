package ru.manannikov.summerpractice_;

import java.time.YearMonth;
import java.util.*;

public class TargetUserModel {

    private Long id;
    private String firstName;
    private String lastName;

    private final List<Long> friendIds = new ArrayList<>();
    private final Map<Long, FriendModel> friendProfiles = new HashMap<>();

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Map<Long, FriendModel> getFriendProfiles() {
        return friendProfiles;
    }

    public List<Long> getFriendIds() {
        return friendIds;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

}
