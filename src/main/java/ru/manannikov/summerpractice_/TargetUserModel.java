package ru.manannikov.summerpractice_;

import java.time.YearMonth;
import java.util.*;

public class TargetUserModel {

    private Long id;
    private String firstName;
    private String lastName;

    private List<Long> friendIds = new ArrayList<>();
    private Map<Long, FriendModel> friendProfiles = new HashMap<>();

    public TargetUserModel() {}

    public TargetUserModel(
            Long id,
            String firstName,
            String lastName,

            List<Long> friendIds,
            Map<Long, FriendModel> friendProfiles
    ) {
        this.firstName = firstName;
        this.friendIds = friendIds;
        this.friendProfiles = friendProfiles;
        this.id = id;
        this.lastName = lastName;
    }

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
