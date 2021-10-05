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

    private Number memSizeMB;
    private Number numCores;
    private Number numPages;
    private Number maxOpenFiles;
    private Number pageSize;
    private boolean numaEnabled;
    private String hostName;
    private String osName;
    private String osVersion;
    private String versionString;
    private String libcVersion;
    private String kernelVersion;
    private long cpuFreqMHz;
    private String cpuArch;
    private String mongodbVersion;
    
    /**
     * @return the memSizeMB
     */
    public Number getMemSizeMB() {
        return memSizeMB;
    }
    /**
     * @param memSizeMB the memSizeMB to set
     */
    public void setMemSizeMB(Number memSizeMB) {
        this.memSizeMB = memSizeMB;
    }
    /**
     * @return the numCores
     */
    public Number getNumCores() {
        return numCores;
    }
    /**
     * @param numCores the numCores to set
     */
    public void setNumCores(Number numCores) {
        this.numCores = numCores;
    }
    /**
     * @return the numPages
     */
    public Number getNumPages() {
        return numPages;
    }
    /**
     * @param numPages the numPages to set
     */
    public void setNumPages(Number numPages) {
        this.numPages = numPages;
    }
    /**
     * @return the maxOpenFiles
     */
    public Number getMaxOpenFiles() {
        return maxOpenFiles;
    }
    /**
     * @param maxOpenFiles the maxOpenFiles to set
     */
    public void setMaxOpenFiles(Number maxOpenFiles) {
        this.maxOpenFiles = maxOpenFiles;
    }
    /**
     * @return the pageSize
     */
    public Number getPageSize() {
        return pageSize;
    }
    /**
     * @param pageSize the pageSize to set
     */
    public void setPageSize(Number pageSize) {
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

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
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
    public long getCpuFreqMHz() {
        return cpuFreqMHz;
    }
    /**
     * @param cpuFreqMHz the cpuFreqMHz to set
     */
    public void setCpuFreqMHz(long cpuFreqMHz) {
        this.cpuFreqMHz = cpuFreqMHz;
    }

    public String getCpuArch() {
        return this.cpuArch;
    }
    public void setCpuArch(String cpuArch) {
        this.cpuArch = cpuArch;
    }

    public String getMongodbVersion() {
        return this.mongodbVersion;
    }

    public void setMongodbVersion(String mongodbVersion) {
        this.mongodbVersion = mongodbVersion;
    }
}
