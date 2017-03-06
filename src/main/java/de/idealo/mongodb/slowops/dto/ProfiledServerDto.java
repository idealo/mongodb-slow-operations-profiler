package de.idealo.mongodb.slowops.dto;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;

import java.util.HashMap;
import java.util.List;

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

    public ProfiledServerDto(boolean enabled, String label, ServerAddress[] hosts, String[] ns, String adminUser, String adminPw, long slowMs) {
        this.enabled = enabled;
        this.label = label;
        this.hosts = hosts;
        this.ns = ns;
        this.adminUser = adminUser;
        this.adminPw = adminPw;
        this.slowMs = slowMs;
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




}
