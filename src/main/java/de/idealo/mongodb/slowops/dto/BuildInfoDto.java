/*
 * Copyright (c) 2015 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.dto;

/**
 * 
 * 
 * @author kay.agahd
 * @since 18.05.2015
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class BuildInfoDto {

    private String version;
    private String gitVersion;
    private String javascriptEngine;
    private String allocator;
    private int bits;
    private int maxBsonObjectSize;
    private double ok;
    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }
    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }
    /**
     * @return the gitVersion
     */
    public String getGitVersion() {
        return gitVersion;
    }
    /**
     * @param gitVersion the gitVersion to set
     */
    public void setGitVersion(String gitVersion) {
        this.gitVersion = gitVersion;
    }
    /**
     * @return the javascriptEngine
     */
    public String getJavascriptEngine() {
        return javascriptEngine;
    }
    /**
     * @param javascriptEngine the javascriptEngine to set
     */
    public void setJavascriptEngine(String javascriptEngine) {
        this.javascriptEngine = javascriptEngine;
    }
    /**
     * @return the allocator
     */
    public String getAllocator() {
        return allocator;
    }
    /**
     * @param allocator the allocator to set
     */
    public void setAllocator(String allocator) {
        this.allocator = allocator;
    }
    /**
     * @return the bits
     */
    public int getBits() {
        return bits;
    }
    /**
     * @param bits the bits to set
     */
    public void setBits(int bits) {
        this.bits = bits;
    }
    /**
     * @return the maxBsonObjectSize
     */
    public int getMaxBsonObjectSize() {
        return maxBsonObjectSize;
    }
    /**
     * @param maxBsonObjectSize the maxBsonObjectSize to set
     */
    public void setMaxBsonObjectSize(int maxBsonObjectSize) {
        this.maxBsonObjectSize = maxBsonObjectSize;
    }
    /**
     * @return the ok
     */
    public double getOk() {
        return ok;
    }
    /**
     * @param ok the ok to set
     */
    public void setOk(double ok) {
        this.ok = ok;
    }
    
    

}
