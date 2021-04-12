package com.hivemq.update.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class UpdateRequest {
    @JsonProperty("hivemq")
    private HiveMQRequest hiveMQRequest;
    @JsonProperty("plugins")
    private List<Plugin> plugins = new ArrayList();
    @JsonProperty("system")
    private SystemInformation systemInformation;

    public HiveMQRequest getHiveMQRequest() {
        return this.hiveMQRequest;
    }

    public void setHiveMQInformation(HiveMQRequest paramHiveMQRequest) {
        this.hiveMQRequest = paramHiveMQRequest;
    }

    public List<Plugin> getPlugins() {
        return this.plugins;
    }

    public void setPlugins(List<Plugin> paramList) {
        this.plugins = paramList;
    }

    public SystemInformation getSystemInformation() {
        return this.systemInformation;
    }

    public void setSystemInformation(SystemInformation paramSystemInformation) {
        this.systemInformation = paramSystemInformation;
    }

    public static class SystemInformation {
        private String javaVersion;
        private String javaVendor;
        private String osArch;
        private String osName;
        private String osVersion;

        public String getJavaVersion() {
            return this.javaVersion;
        }

        public void setJavaVersion(String paramString) {
            this.javaVersion = paramString;
        }

        public String getJavaVendor() {
            return this.javaVendor;
        }

        public void setJavaVendor(String paramString) {
            this.javaVendor = paramString;
        }

        public String getOsArch() {
            return this.osArch;
        }

        public void setOsArch(String paramString) {
            this.osArch = paramString;
        }

        public String getOsName() {
            return this.osName;
        }

        public void setOsName(String paramString) {
            this.osName = paramString;
        }

        public String getOsVersion() {
            return this.osVersion;
        }

        public void setOsVersion(String paramString) {
            this.osVersion = paramString;
        }
    }

    public static class Plugin {
        private String version;
        private String name;

        public String getVersion() {
            return this.version;
        }

        public void setVersion(String paramString) {
            this.version = paramString;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String paramString) {
            this.name = paramString;
        }
    }

    public static class HiveMQRequest {
        private String id;
        private String version;

        public String getVersion() {
            return this.version;
        }

        public void setVersion(String paramString) {
            this.version = paramString;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String paramString) {
            this.id = paramString;
        }
    }
}
