package com.logmonitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class AppLogFetchRequest {

    @NotNull
    private Long logConfigId;

    @NotBlank
    @Pattern(regexp = "CURRENT|ARCHIVED")
    private String mode;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    private String logDate;

    public Long getLogConfigId() { return logConfigId; }
    public void setLogConfigId(Long logConfigId) { this.logConfigId = logConfigId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getLogDate() { return logDate; }
    public void setLogDate(String logDate) { this.logDate = logDate; }
}
