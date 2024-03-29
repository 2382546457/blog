package com.xiaohe.filter;


import cn.hutool.http.HttpStatus;
import com.alibaba.excel.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohe.constants.Constants;

import com.xiaohe.domain.entity.LoginUser;
import com.xiaohe.domain.entity.User;
import com.xiaohe.mapper.MenuMapper;
import com.xiaohe.utils.Result;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-01-06 19:39
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MenuMapper menuMapper;

    public TokenAuthenticationFilter(StringRedisTemplate stringRedisTemplate, MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader(Constants.User.AUTHENTICATION_NAME);
        // 如果token里面没有值，说明这个接口不是这个filter需要拦截的，直接放行
        if (StringUtils.isEmpty(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        String json = stringRedisTemplate.opsForValue().get(Constants.User.ADMIN_LOGIN_TOKEN + token);
        // 如果根据token拿不到json，有两种情况:
        // 1. token过期，用户不知道
        // 2. 用户有token ，这个token是自己造的。
        if (Objects.isNull(json)) {
            response.setContentType(Constants.CONTENT_TYPE.APPLICATION_JSON);
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(Result.error("身份过期，请重新登录!", HttpStatus.HTTP_FORBIDDEN)));
            return ;
        }
        // 如果由token能找到json，证明30mins之前登陆过。刷新该token的过期时间
        stringRedisTemplate.expire(Constants.User.ADMIN_LOGIN_TOKEN + token, 30, TimeUnit.MINUTES);

        // 放入SecurityContextHolder中，后续使用
        User user = objectMapper.readValue(json, User.class);
        List<String> list = menuMapper.selectPermsByUserId(user.getId());
        LoginUser loginUser = new LoginUser(user, list);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
