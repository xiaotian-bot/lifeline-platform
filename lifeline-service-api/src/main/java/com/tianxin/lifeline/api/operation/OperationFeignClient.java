package com.tianxin.lifeline.api.operation;

import com.tianxin.lifeline.api.operation.dto.OrderCreateDTO;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 运维工单服务（operation-service）Feign 客户端。
 *
 * <p>供预警/预案等上游服务触发工单创建。
 */
@FeignClient(name = "operation-service", path = "/api/v1/orders")
public interface OperationFeignClient {

    /**
     * 创建运维工单。
     *
     * @param orderCreateDTO 工单创建参数
     * @return 创建成功的工单ID
     */
    @PostMapping
    Result<Long> createOrder(@RequestBody OrderCreateDTO orderCreateDTO);
}
