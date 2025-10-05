// src/main/java/com/kmu/syncpos/dto/dashboard/DashboardKPIs.java
package com.kmu.syncpos.dto.dashboard;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class DashboardKPIs {
    private final double todaysSalesTotal;
    private final int todaysTransactionCount;

    public DashboardKPIs(double todaysSalesTotal, int todaysTransactionCount) {
        this.todaysSalesTotal = todaysSalesTotal;
        this.todaysTransactionCount = todaysTransactionCount;
    }


}