package de.idealo.mongodb.slowops.dto;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by kay.agahd on 26.10.16.
 */
public class ProfiledServerDto {

    private boolean enabled;
    private String label;
    private ServerAddress[] hosts;
    private String[] ns;
    private String adminUser;
    private String adminPw;
    private long slowMs;
    private List<Future<List<ServerAddress>>> futureResolvedHostLists;//each defined access point will result in a future List of ServerAddresses

    public ProfiledServerDto(boolean enabled, String label, ServerAddress[] hosts, String[] ns, String adminUser, String adminPw, long slowMs) {
        this.enabled = enabled;
        this.label = label;
        this.hosts = hosts;
        this.ns = ns;
        this.adminUser = adminUser;
        this.adminPw = adminPw;
        this.slowMs = slowMs;
        this.futureResolvedHostLists = new ArrayList<>();
    }

    public HashMap<String, List<String>> getCollectionsPerDatabase(){
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        for(String n : ns){
            String db = null;
            String col = null;
            String[] parts = n.split("\\.");
            if(n.length() > 1){
                db = parts[0];
                col = n.substring(n.indexOf(".")+1);
            }
            List<String> colls = result.get(db);
            if(colls == null){
                colls = Lists.newArrayList();
            }
            colls.add(col);
            result.put(db, colls);
        }
        return result;
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

    public long getSlowMs() { return slowMs; }

    public void setSlowMs(long slowMs) { this.slowMs = slowMs; }

    public List<Future<List<ServerAddress>>> getFutureResolvedHostLists() {
        return futureResolvedHostLists;
    }

    public void addFutureResolvedHostList(Future<List<ServerAddress>> resolvedHosts) {
        this.futureResolvedHostLists.add(resolvedHosts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfiledServerDto that = (ProfiledServerDto) o;

        if (enabled != that.enabled) return false;
        if (slowMs != that.slowMs) return false;
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
        result = 31 * result + (int) (slowMs ^ (slowMs >>> 32));
        return result;
    }
}
