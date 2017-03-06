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
public class HostInfoDto {

    private int memSizeMB;
    private int numCores;
    private int numPages;
    private int maxOpenFiles;
    private long pageSize;
    private boolean numaEnabled;
    private String osName;
    private String osVersion;
    private String versionString;
    private String libcVersion;
    private String kernelVersion;
    private String cpuFreqMHz;
    
    /**
     * @return the memSizeMB
     */
    public int getMemSizeMB() {
        return memSizeMB;
    }
    /**
     * @param memSizeMB the memSizeMB to set
     */
    public void setMemSizeMB(int memSizeMB) {
        this.memSizeMB = memSizeMB;
    }
    /**
     * @return the numCores
     */
    public int getNumCores() {
        return numCores;
    }
    /**
     * @param numCores the numCores to set
     */
    public void setNumCores(int numCores) {
        this.numCores = numCores;
    }
    /**
     * @return the numPages
     */
    public int getNumPages() {
        return numPages;
    }
    /**
     * @param numPages the numPages to set
     */
    public void setNumPages(int numPages) {
        this.numPages = numPages;
    }
    /**
     * @return the maxOpenFiles
     */
    public int getMaxOpenFiles() {
        return maxOpenFiles;
    }
    /**
     * @param maxOpenFiles the maxOpenFiles to set
     */
    public void setMaxOpenFiles(int maxOpenFiles) {
        this.maxOpenFiles = maxOpenFiles;
    }
    /**
     * @return the pageSize
     */
    public long getPageSize() {
        return pageSize;
    }
    /**
     * @param pageSize the pageSize to set
     */
    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }
    /**
     * @return the numaEnabled
     */
    public boolean isNumaEnabled() {
        return numaEnabled;
    }
    /**
     * @param numaEnabled the numaEnabled to set
     */
    public void setNumaEnabled(boolean numaEnabled) {
        this.numaEnabled = numaEnabled;
    }
    /**
     * @return the osName
     */
    public String getOsName() {
        return osName;
    }
    /**
     * @param osName the osName to set
     */
    public void setOsName(String osName) {
        this.osName = osName;
    }
    /**
     * @return the osVersion
     */
    public String getOsVersion() {
        return osVersion;
    }
    /**
     * @param osVersion the osVersion to set
     */
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }
    /**
     * @return the versionString
     */
    public String getVersionString() {
        return versionString;
    }
    /**
     * @param versionString the versionString to set
     */
    public void setVersionString(String versionString) {
        this.versionString = versionString;
    }
    /**
     * @return the libcVersion
     */
    public String getLibcVersion() {
        return libcVersion;
    }
    /**
     * @param libcVersion the libcVersion to set
     */
    public void setLibcVersion(String libcVersion) {
        this.libcVersion = libcVersion;
    }
    /**
     * @return the kernelVersion
     */
    public String getKernelVersion() {
        return kernelVersion;
    }
    /**
     * @param kernelVersion the kernelVersion to set
     */
    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }
    /**
     * @return the cpuFreqMHz
     */
    public String getCpuFreqMHz() {
        return cpuFreqMHz;
    }
    /**
     * @param cpuFreqMHz the cpuFreqMHz to set
     */
    public void setCpuFreqMHz(String cpuFreqMHz) {
        this.cpuFreqMHz = cpuFreqMHz;
    }
    
    

}
