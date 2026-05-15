package com.panda.interceptor;

import cn.dev33.satoken.stp.StpLogic;
import com.panda.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SaTokenAdminInterceptor implements HandlerInterceptor {

    private final StpLogic adminStpLogic;

    public SaTokenAdminInterceptor(@Qualifier("adminStpLogic") StpLogic adminStpLogic) {
        this.adminStpLogic = adminStpLogic;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        adminStpLogic.checkLogin();
        BaseContext.setCurrentId(Long.valueOf(adminStpLogic.getLoginIdAsString()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}
