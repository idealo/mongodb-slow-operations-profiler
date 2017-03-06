package de.idealo.mongodb.slowops.dto;

import com.mongodb.ServerAddress;

/**
 * Created by kay.agahd on 26.10.16.
 */
public class CollectorServerDto {

    private ServerAddress[] hosts;
    private String db;
    private String collection;
    private String adminUser;
    private String adminPw;

    public CollectorServerDto(ServerAddress[] hosts, String db, String collection, String adminUser, String adminPw) {
        this.hosts = hosts;
        this.db = db;
        this.collection = collection;
        this.adminUser = adminUser;
        this.adminPw = adminPw;
    }

    public ServerAddress[] getHosts() {
        return hosts;
    }

    public void setHosts(ServerAddress[] hosts) {
        this.hosts = hosts;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public String getAdminPw() {
        return adminPw;
    }

    public void setAdminPw(String adminPw) {
        this.adminPw = adminPw;
    }




}
