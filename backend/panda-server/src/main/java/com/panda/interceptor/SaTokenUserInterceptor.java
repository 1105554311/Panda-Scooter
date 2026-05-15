package com.panda.interceptor;

import cn.dev33.satoken.stp.StpLogic;
import com.panda.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SaTokenUserInterceptor implements HandlerInterceptor {

    private final StpLogic userStpLogic;

    public SaTokenUserInterceptor(@Qualifier("userStpLogic") StpLogic userStpLogic) {
        this.userStpLogic = userStpLogic;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        userStpLogic.checkLogin();
        BaseContext.setCurrentId(Long.valueOf(userStpLogic.getLoginIdAsString()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}
