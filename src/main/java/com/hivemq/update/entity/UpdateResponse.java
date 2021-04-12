package com.hivemq.update.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class UpdateResponse {
    @JsonProperty("hivemq")
    private HiveMQResponse hiveMQResponse;
    @JsonProperty("plugins")
    private List<Plugin> plugins = new ArrayList();

    public HiveMQResponse getHiveMQResponse() {
        return this.hiveMQResponse;
    }

    public void setHiveMQInformation(HiveMQResponse paramHiveMQResponse) {
        this.hiveMQResponse = paramHiveMQResponse;
    }

    public List<Plugin> getPlugins() {
        return this.plugins;
    }

    public void setPlugins(List<Plugin> paramList) {
        this.plugins = paramList;
    }

    public static class Plugin {
        private String level;
        private String type;
        private String url;
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

        public String getLevel() {
            return this.level;
        }

        public void setLevel(String paramString) {
            this.level = paramString;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String paramString) {
            this.type = paramString;
        }

        public String getUrl() {
            return this.url;
        }

        public void setUrl(String paramString) {
            this.url = paramString;
        }
    }

    public static class HiveMQResponse {
        private String level;
        private String type;
        private String version;
        private String url;

        public String getLevel() {
            return this.level;
        }

        public void setLevel(String paramString) {
            this.level = paramString;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String paramString) {
            this.type = paramString;
        }

        public String getVersion() {
            return this.version;
        }

        public void setVersion(String paramString) {
            this.version = paramString;
        }

        public String getUrl() {
            return this.url;
        }

        public void setUrl(String paramString) {
            this.url = paramString;
        }
    }
}
