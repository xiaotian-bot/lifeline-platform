package com.tianxin.lifeline.common.core.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一分页查询请求参数。
 *
 * <p>各服务分页接口统一继承或组合使用该对象，页码从 1 开始。
 */
@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 默认排序方向 */
    public static final String ORDER_ASC = "asc";
    public static final String ORDER_DESC = "desc";

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    /** 每页条数，默认 20，最大 500 */
    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 500, message = "每页条数不能超过500")
    private Integer size = 20;

    /** 排序字段（业务字段名，服务端需做白名单校验） */
    private String orderBy;

    /** 排序方向：asc 升序 / desc 降序，默认 desc */
    private String orderDir = ORDER_DESC;

    /**
     * 计算分页偏移量（用于 SQL offset）。
     *
     * @return 偏移记录数
     */
    public long offset() {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : size;
        return (long) (p - 1) * s;
    }

    /**
     * 判断排序方向是否为升序。
     *
     * @return true 表示升序
     */
    public boolean isAsc() {
        return ORDER_ASC.equalsIgnoreCase(orderDir);
    }
}
