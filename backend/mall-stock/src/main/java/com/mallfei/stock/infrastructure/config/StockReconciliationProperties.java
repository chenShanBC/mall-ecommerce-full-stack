package com.mallfei.stock.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mall.stock.reconciliation")
public class StockReconciliationProperties {

    private boolean enabled = true;
    private String dayCron = "0 0 */2 * * ?";
    private String nightCron = "0 */15 0-6 * * ?";
    private boolean onlyRecordInconsistent = true;
    private boolean skipPendingInconsistent = true;
    private int dayMaxSkuPerRun = 100;
    private int nightMaxSkuPerRun = 1000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDayCron() { return dayCron; }
    public void setDayCron(String dayCron) { this.dayCron = dayCron; }
    public String getNightCron() { return nightCron; }
    public void setNightCron(String nightCron) { this.nightCron = nightCron; }
    public boolean isOnlyRecordInconsistent() { return onlyRecordInconsistent; }
    public void setOnlyRecordInconsistent(boolean onlyRecordInconsistent) { this.onlyRecordInconsistent = onlyRecordInconsistent; }
    public boolean isSkipPendingInconsistent() { return skipPendingInconsistent; }
    public void setSkipPendingInconsistent(boolean skipPendingInconsistent) { this.skipPendingInconsistent = skipPendingInconsistent; }
    public int getDayMaxSkuPerRun() { return dayMaxSkuPerRun; }
    public void setDayMaxSkuPerRun(int dayMaxSkuPerRun) { this.dayMaxSkuPerRun = dayMaxSkuPerRun; }
    public int getNightMaxSkuPerRun() { return nightMaxSkuPerRun; }
    public void setNightMaxSkuPerRun(int nightMaxSkuPerRun) { this.nightMaxSkuPerRun = nightMaxSkuPerRun; }
}
