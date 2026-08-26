package com.zl.demo.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.zl.demo.config.DemoProperties;

/**
 * ZL-OPENID 简单认证（MVC 版）
 * <p>标准 Servlet Filter，在 Controller 之前执行：
 * 读取请求头 ZL-OPENID → 白名单校验 → 通过则放行，失败返回 403。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ZlOpenIdFilter implements Filter {

    private final DemoProperties props;

    public ZlOpenIdFilter(DemoProperties props) {
        this.props = props;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 无需初始化
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 开关关闭 → 直接放行
        if (!props.getOpenid().isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        // 绕过路径（如内置 Mock 上游的自调用）
        String path = req.getRequestURI();
        for (String bypass : props.getOpenid().getBypassPaths()) {
            if (path.startsWith(bypass)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 白名单校验
        String openId = req.getHeader("ZL-OPENID");
        if (openId == null || !props.getOpenid().getWhitelist().contains(openId)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resp.getWriter().write("{\"code\":403,\"message\":\"ZL-OPENID invalid or missing\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 无需清理资源
    }
}
