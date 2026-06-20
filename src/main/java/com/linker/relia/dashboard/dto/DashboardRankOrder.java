package com.linker.relia.dashboard.dto;

public enum DashboardRankOrder {
    TOP("asc"),
    BOTTOM("desc");

    private final String sqlDirection;

    DashboardRankOrder(String sqlDirection) {
        this.sqlDirection = sqlDirection;
    }

    public String sqlDirection() {
        return sqlDirection;
    }
}
