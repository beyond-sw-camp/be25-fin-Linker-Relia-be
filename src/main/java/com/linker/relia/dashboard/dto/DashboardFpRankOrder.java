package com.linker.relia.dashboard.dto;

public enum DashboardFpRankOrder {
    TOP("asc"),
    BOTTOM("desc");

    private final String sqlDirection;

    DashboardFpRankOrder(String sqlDirection) {
        this.sqlDirection = sqlDirection;
    }

    public String sqlDirection() {
        return sqlDirection;
    }
}
