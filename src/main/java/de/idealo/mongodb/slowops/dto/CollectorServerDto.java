package de.idealo.mongodb.slowops.dto;

import com.mongodb.ServerAddress;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Arrays;

/**
 * Created by kay.agahd on 26.10.16.
 */
public class CollectorServerDto {

    private ServerAddress[] hosts;
    private String db;
    private String collection;
    private String adminUser;
    private String adminPw;
    private boolean ssl;

    public CollectorServerDto(@JsonProperty("hosts") ServerAddress[] hosts,
                              @JsonProperty("db") String db,
                              @JsonProperty("collection") String collection,
                              @JsonProperty("adminUser") String adminUser,
                              @JsonProperty("adminPw") String adminPw,
                              @JsonProperty("ssl") boolean ssl) {
        this.hosts = hosts;
        this.db = db;
        this.collection = collection;
        this.adminUser = adminUser;
        this.adminPw = adminPw;
        this.ssl = ssl;
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

    public boolean getSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollectorServerDto that = (CollectorServerDto) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(hosts, that.hosts)) return false;
        if (!db.equals(that.db)) return false;
        if (!collection.equals(that.collection)) return false;
        if (ssl != that.ssl) return false;
        if (adminUser != null ? !adminUser.equals(that.adminUser) : that.adminUser != null) return false;
        return adminPw != null ? adminPw.equals(that.adminPw) : that.adminPw == null;

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(hosts);
        result = 31 * result + db.hashCode();
        result = 31 * result + collection.hashCode();
        result = 31 * result + (adminUser != null ? adminUser.hashCode() : 0);
        result = 31 * result + (adminPw != null ? adminPw.hashCode() : 0);
        return result;
    }
}
