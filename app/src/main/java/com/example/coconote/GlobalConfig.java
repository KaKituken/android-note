package com.example.coconote;

public class GlobalConfig {
    private static final GlobalConfig instance = new GlobalConfig();

    private String baseUrl = "https://1b8e2c28-7ad1-4d5b-b139-a0996b2c0ac1.mock.pstmn.io"; // 默认IP地址

    private GlobalConfig() {}

    public static GlobalConfig getInstance() {
        return instance;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}

