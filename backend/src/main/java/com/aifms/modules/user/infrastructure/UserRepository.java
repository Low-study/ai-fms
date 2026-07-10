package com.aifms.modules.user.infrastructure;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * {@link UserEntity} 的 R2DBC 数据访问接口。
 * 所有查询方法均自动排除软删除（status = 'DELETED'）的记录。
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<UserEntity, UUID> {

    /**
     * 按用户名精确查找（排除已删除用户）。
     *
     * @param username 用户名
     * @return 匹配的实体（可能为空）
     */
    @Query("SELECT * FROM users WHERE username = :username AND status != 'DELETED'")
    Mono<UserEntity> findByUsername(@Param("username") String username);

    /**
     * 按邮箱精确查找（排除已删除用户）。
     *
     * @param email 邮箱地址
     * @return 匹配的实体（可能为空）
     */
    @Query("SELECT * FROM users WHERE email = :email AND status != 'DELETED'")
    Mono<UserEntity> findByEmail(@Param("email") String email);

    /**
     * 按 ID 查找非删除用户。
     *
     * @param id 用户 ID
     * @return 匹配的实体（可能为空）
     */
    @Query("SELECT * FROM users WHERE id = :id AND status != 'DELETED'")
    Mono<UserEntity> findByIdNonDeleted(@Param("id") UUID id);

    /**
     * 分页搜索非删除用户。
     * 关键字在 username、email、display_name 三个字段中做模糊匹配（ILIKE）。
     *
     * @param keyword 搜索关键字（null 或空字符串表示不筛选）
     * @param size    每页大小
     * @param offset  偏移量
     * @return 匹配的用户列表
     */
    @Query("""
        SELECT * FROM users
        WHERE status != 'DELETED'
          AND (:keyword IS NULL
               OR username   ILIKE '%' || :keyword || '%'
               OR email      ILIKE '%' || :keyword || '%'
               OR display_name ILIKE '%' || :keyword || '%')
        ORDER BY created_at DESC
        LIMIT :size OFFSET :offset
        """)
    Flux<UserEntity> findAllNonDeleted(@Param("keyword") String keyword,
                                       @Param("size") long size,
                                       @Param("offset") long offset);

    /**
     * 统计非删除用户数量（带关键字筛选）。
     *
     * @param keyword 搜索关键字（null 或空字符串表示全部）
     * @return 匹配的记录数
     */
    @Query("""
        SELECT COUNT(*) FROM users
        WHERE status != 'DELETED'
          AND (:keyword IS NULL
               OR username   ILIKE '%' || :keyword || '%'
               OR email      ILIKE '%' || :keyword || '%'
               OR display_name ILIKE '%' || :keyword || '%')
        """)
    Mono<Long> countNonDeleted(@Param("keyword") String keyword);
}
