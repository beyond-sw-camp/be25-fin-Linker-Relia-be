package com.linker.relia.user.repository;

import com.linker.relia.user.domain.FpMonthlyInfo;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FpMonthlyInfoRepository extends JpaRepository<FpMonthlyInfo, UUID> {

    // 같은 지점 최신 마감 기준 설계사 목록
    // 스코어링에서 추천 후보 가져올 때 사용
    @Query("""
        SELECT f FROM FpMonthlyInfo f
        WHERE f.organizationCode = :organizationCode
        AND f.closingMonth = (
            SELECT MAX(f2.closingMonth)
            FROM FpMonthlyInfo f2
            WHERE f2.empCode = f.empCode
        )
    """)
    List<FpMonthlyInfo> findLatestByOrganizationCode(
            @Param("organizationCode") String organizationCode
    );
}