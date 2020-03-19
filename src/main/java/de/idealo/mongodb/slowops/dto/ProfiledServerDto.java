package de.idealo.mongodb.slowops.dto;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;
import de.idealo.mongodb.slowops.util.MongoResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by kay.agahd on 26.10.16.
 */
public class ProfiledServerDto {

    private static final Logger LOG = LoggerFactory.getLogger(ProfiledServerDto.class);

    private boolean enabled;
    private String label;
    private ServerAddress[] hosts;
    private String[] ns;
    private String adminUser;
    private String adminPw;
    private boolean ssl;
    private long slowMs;
    private int responseTimeout;
    private HashSet<ServerAddress> resolvedHosts;
    private List<String> resolvedDatabases;
    private List<String> excludedDbs;
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    private final Lock readLock = globalLock.readLock();
    private final Lock writeLock = globalLock.writeLock();

    public ProfiledServerDto(boolean enabled, String label, ServerAddress[] hosts, String[] ns, String adminUser, String adminPw, boolean ssl, long slowMs, int responseTimeout, List<String> excludedDbs) {
        this.enabled = enabled;
        this.label = label;
        this.hosts = hosts;
        this.ns = ns;
        this.adminUser = adminUser;
        this.adminPw = adminPw;
        this.ssl = ssl;
        this.slowMs = slowMs;
        this.responseTimeout = responseTimeout;
        this.resolvedHosts = new HashSet<ServerAddress>();
        this.resolvedDatabases = Lists.newLinkedList();
        this.excludedDbs = excludedDbs;
    }

    /**
     *
     * @return HashMap whose keys are database names and their value is a list of collection names
     */
    public HashMap<String, List<String>> getCollectionsPerDatabase(){
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        for(String n : ns){
            String db = null;
            String col = null;
            String[] parts = n.split("\\.");
            if(n.length() > 1){
                db = parts[0];
                col = n.substring(n.indexOf(".")+1);
                if("*".equals(db)){
                    try {
                        readLock.lock();
                        for (String dbName: resolvedDatabases) {
                            addCollection(dbName, col, result);
                        }
                    } finally {
                        readLock.unlock();
                    }

                }else if(db.startsWith("!")){
                    String dbToRemove = db.substring(1);
                    try {
                        readLock.lock();
                        result.remove(dbToRemove);
                    } finally {
                        readLock.unlock();
                    }

                }else{
                    addCollection(db, col, result);
                }
            }
        }

        for(String exDb : excludedDbs){
            result.remove(exDb);
        }

        return result;
    }


    private void addCollection(String db, String col, HashMap<String, List<String>> collsPerDb){
        List<String> colls = collsPerDb.get(db);
        if(colls == null){
            colls = Lists.newArrayList();
        }
        colls.add(col);
        collsPerDb.put(db, colls);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ServerAddress[] getHosts() {
        return hosts;
    }

    public void setHosts(ServerAddress[] hosts) {
        this.hosts = hosts;
    }

    public String[] getNs() {
        return ns;
    }

    public void setNs(String[] ns) {
        this.ns = ns;
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

    public void setAdminPw(String adminPw) { this.adminPw = adminPw; }

    public boolean getSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) { this.ssl = ssl; }

    public long getSlowMs() { return slowMs; }

    public void setSlowMs(long slowMs) { this.slowMs = slowMs; }

    public int getResponseTimeout() { return responseTimeout; }

    public void setResponseTimeout(int responseTimeout) { this.responseTimeout=responseTimeout; }

    public HashSet<ServerAddress> getResolvedHosts() {
        return resolvedHosts;
    }

    public void setResolvedResults(MongoResolver mongoResolver) {
        try {
            writeLock.lock();
            resolvedHosts.addAll(mongoResolver.getResolvedHosts());
            resolvedDatabases.addAll(mongoResolver.getResolvedDatabases());
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfiledServerDto that = (ProfiledServerDto) o;

        if (enabled != that.enabled) return false;
        if (slowMs != that.slowMs) return false;
        if (responseTimeout != that.responseTimeout) return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        if (!Arrays.equals(hosts, that.hosts)) return false;
        if (!Arrays.equals(ns, that.ns)) return false;
        if (adminUser != null ? !adminUser.equals(that.adminUser) : that.adminUser != null) return false;
        return adminPw != null ? adminPw.equals(that.adminPw) : that.adminPw == null;

    }

    @Override
    public int hashCode() {
        int result = (enabled ? 1 : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(hosts);
        result = 31 * result + Arrays.hashCode(ns);
        result = 31 * result + (adminUser != null ? adminUser.hashCode() : 0);
        result = 31 * result + (adminPw != null ? adminPw.hashCode() : 0);
        result = 31 * result + (Boolean.hashCode(ssl));
        result = 31 * result + (int) (slowMs ^ (slowMs >>> 32));
        result = 31 * result + (responseTimeout ^ (responseTimeout >>> 32));
        return result;
    }
}
