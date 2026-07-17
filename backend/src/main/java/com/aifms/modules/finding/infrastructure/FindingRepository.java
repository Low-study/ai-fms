package com.aifms.modules.finding.infrastructure;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * {@link FindingEntity} 的 R2DBC 数据访问接口。
 * 所有查询方法均自动排除已关闭（status = 'CLOSED'）的记录。
 */
@Repository
public interface FindingRepository extends ReactiveCrudRepository<FindingEntity, UUID> {

    /**
     * 按标题精确查找（排除已关闭指摘）。
     *
     * @param title 指摘标题
     * @return 匹配的实体（可能为空）
     */
    @Query("SELECT * FROM findings WHERE title = :title AND status != 'CLOSED'")
    Mono<FindingEntity> findByTitleNonClosed(@Param("title") String title);

    /**
     * 按 ID 查找非关闭指摘。
     *
     * @param id 指摘 ID
     * @return 匹配的实体（可能为空）
     */
    @Query("SELECT * FROM findings WHERE id = :id AND status != 'CLOSED'")
    Mono<FindingEntity> findByIdNonClosed(@Param("id") UUID id);

    /**
     * 分页搜索非关闭指摘。
     * 关键字在 title、description、category 三个字段中做模糊匹配（ILIKE）。
     *
     * @param keyword 搜索关键字（null 或空字符串表示不筛选）
     * @param size    每页大小
     * @param offset  偏移量
     * @return 匹配的指摘列表
     */
    @Query("""
        SELECT * FROM findings
        WHERE status != 'CLOSED'
          AND (:keyword IS NULL
               OR title       ILIKE '%' || :keyword || '%'
               OR description ILIKE '%' || :keyword || '%'
               OR category    ILIKE '%' || :keyword || '%')
        ORDER BY created_at DESC
        LIMIT :size OFFSET :offset
        """)
    Flux<FindingEntity> findAllNonClosed(@Param("keyword") String keyword,
                                          @Param("size") long size,
                                          @Param("offset") long offset);

    /**
     * 统计非关闭指摘数量（带关键字筛选）。
     *
     * @param keyword 搜索关键字（null 或空字符串表示全部）
     * @return 匹配的记录数
     */
    @Query("""
        SELECT COUNT(*) FROM findings
        WHERE status != 'CLOSED'
          AND (:keyword IS NULL
               OR title       ILIKE '%' || :keyword || '%'
               OR description ILIKE '%' || :keyword || '%'
               OR category    ILIKE '%' || :keyword || '%')
        """)
    Mono<Long> countNonClosed(@Param("keyword") String keyword);
}
