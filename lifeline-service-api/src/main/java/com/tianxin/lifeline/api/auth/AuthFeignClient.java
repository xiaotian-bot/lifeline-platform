package com.tianxin.lifeline.api.auth;

import com.tianxin.lifeline.api.auth.dto.MenuDTO;
import com.tianxin.lifeline.api.auth.dto.UserInfoDTO;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 系统用户权限服务（sys-auth-service）Feign 客户端。
 *
 * <p>供其他微服务获取用户信息与菜单权限。
 */
@FeignClient(name = "sys-auth-service", path = "/api/v1/auth")
public interface AuthFeignClient {

    /**
     * 根据用户ID获取用户信息。
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/users/{userId}")
    Result<UserInfoDTO> getUserById(@PathVariable("userId") Long userId);

    /**
     * 获取用户的菜单权限树。
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @GetMapping("/users/{userId}/menus")
    Result<List<MenuDTO>> getUserMenus(@PathVariable("userId") Long userId);

    /**
     * 获取用户的角色编码列表。
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    @GetMapping("/users/{userId}/roles")
    Result<List<String>> getUserRoles(@PathVariable("userId") Long userId);
}
