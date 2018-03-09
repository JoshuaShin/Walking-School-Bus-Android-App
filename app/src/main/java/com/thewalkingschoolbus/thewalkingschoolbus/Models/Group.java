package com.thewalkingschoolbus.thewalkingschoolbus.Models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jackyx on 2018-03-08.
 */

public class Group {
    private String groupDescription;
    private User leader;
    private List<User> memberUsers;
    private float[] routeLatArray;
    private float[] routeLngArray;
    private String href;

    public Group(){
        groupDescription = null;
        leader=null;
        memberUsers = new ArrayList<>();
        routeLatArray = new float[3];
        routeLngArray = new float[3];
        href = null;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }

    public User getLeader() {
        return leader;
    }

    public void setLeader(User leader) {
        this.leader = leader;
    }

    public List<User> getMemberUsers() {
        return memberUsers;
    }

    public void setMemberUsers(List<User> memberUsers) {
        this.memberUsers = memberUsers;
    }

    public float[] getRouteLatArray() {
        return routeLatArray;
    }

    public void setRouteLatArray(float[] routeLatArray) {
        this.routeLatArray = routeLatArray;
    }

    public float[] getRouteLngArray() {
        return routeLngArray;
    }

    public void setRouteLngArray(float[] routeLngArray) {
        this.routeLngArray = routeLngArray;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

}
