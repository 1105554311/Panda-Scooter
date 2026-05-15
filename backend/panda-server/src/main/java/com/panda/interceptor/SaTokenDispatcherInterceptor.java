package com.panda.interceptor;

import cn.dev33.satoken.stp.StpLogic;
import com.panda.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SaTokenDispatcherInterceptor implements HandlerInterceptor {

    private final StpLogic dispatcherStpLogic;

    public SaTokenDispatcherInterceptor(@Qualifier("dispatcherStpLogic") StpLogic dispatcherStpLogic) {
        this.dispatcherStpLogic = dispatcherStpLogic;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        dispatcherStpLogic.checkLogin();
        BaseContext.setCurrentId(Long.valueOf(dispatcherStpLogic.getLoginIdAsString()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}
