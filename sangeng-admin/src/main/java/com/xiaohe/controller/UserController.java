package com.xiaohe.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohe.constants.Constants;
import com.xiaohe.domain.entity.LoginUser;
import com.xiaohe.domain.entity.Menu;
import com.xiaohe.domain.entity.User;
import com.xiaohe.domain.vo.AdminLoginUserVo;
import com.xiaohe.domain.vo.AdminUserInfoVo;
import com.xiaohe.domain.vo.RoutersVo;
import com.xiaohe.domain.vo.UserInfoVo;
import com.xiaohe.service.MenuService;
import com.xiaohe.service.RoleService;
import com.xiaohe.utils.JWTUtils;
import com.xiaohe.utils.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@RestController
public class UserController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MenuService menuService;

    @PostMapping("/user/login")
    public Result login(@RequestBody AdminLoginUserVo user) throws JsonProcessingException {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getUserName(), user.getPassword());

        Authentication authenticate = authenticationManager.authenticate(authentication);
        if (Objects.isNull(authenticate) || Objects.isNull(authentication.getPrincipal())) {
            return Result.error("?????????/????????????");
        }
        ObjectMapper objectMapper = new ObjectMapper();

        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        User user1 = loginUser.getUser();
        user1.setPassword(null);
        // ????????????????????????token
        // token?????????json?????? ??????redis
        String json = objectMapper.writeValueAsString(user1);
        String token = JWTUtils.generateToken(objectMapper.readValue(json, Map.class));
        // ??????redis?????? token-json????????????, token???????????????30mins
        stringRedisTemplate.opsForValue().set(Constants.User.ADMIN_LOGIN_TOKEN + token, json, 30, TimeUnit.MINUTES);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ???token??????
        Map<String, String> map = new HashMap<>();
        map.put(Constants.User.AUTHENTICATION_NAME, token);
        return Result.success(map, "????????????");
    }

    @PostMapping("/user/logout")
    public Result logout(HttpServletRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(Objects.isNull(principal)) {
            return Result.error("???????????????????????????");
        }
        // ??????token, ??????redis??????token
        String token = request.getHeader(Constants.User.AUTHENTICATION_NAME);
        Boolean delete = stringRedisTemplate.delete(Constants.User.ADMIN_LOGIN_TOKEN + token);
        if (!delete) {
            return Result.error("????????????!");
        }
        return Result.success("????????????");
    }


    @GetMapping("getInfo")
    public Result getInfo() {
        // ???SecurityContext???????????????????????????????????????
        User user = ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
        Long id = user.getId();
        // ????????????id??????????????????
        List<String> permissions = menuService.getAccessPathByUserId(id);

        // ????????????id????????????roleKey
        List<String> roleKey = roleService.getRoleKeyByUserId(id);


        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(user, userInfoVo);
        return Result.success(new AdminUserInfoVo(permissions, roleKey, userInfoVo));

    }

    @GetMapping("getRouters")
    public Result getRouters() {
        User user = ((LoginUser) (SecurityContextHolder.getContext().getAuthentication().getPrincipal())).getUser();
        Long id = user.getId();
        List<Menu> routers = menuService.getRouterByUserId(id);
        return Result.success(new RoutersVo(routers));
    }
}
