package com.tianxin.lifeline.common.core.page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页查询响应结构。
 *
 * <p>格式：{ "records": [], "total": 100, "page": 1, "size": 20 }
 *
 * @param <T> 记录数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页记录列表 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Integer page;

    /** 每页条数 */
    private Integer size;

    /**
     * 构建分页结果。
     *
     * @param records 当前页记录
     * @param total   总记录数
     * @param page    当前页码
     * @param size    每页条数
     * @param <T>     记录类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        return new PageResult<>(records, total, page, size);
    }

    /**
     * 构建空分页结果。
     *
     * @param page 当前页码
     * @param size 每页条数
     * @param <T>  记录类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size);
    }

    /**
     * 记录类型转换（保持分页信息不变）。
     *
     * @param mapper 记录转换函数
     * @param <R>    目标记录类型
     * @return 转换后的分页结果
     */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> converted = records == null
                ? Collections.emptyList()
                : records.stream().map(mapper).toList();
        return new PageResult<>(converted, total == null ? 0L : total, page, size);
    }
}
