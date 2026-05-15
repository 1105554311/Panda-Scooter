package com.panda.config;

import cn.dev33.satoken.stp.StpLogic;
import com.panda.auth.AuthLoginType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthConfiguration {

    @Bean("userStpLogic")
    public StpLogic userStpLogic() {
        return new StpLogic(AuthLoginType.USER);
    }

    @Bean("dispatcherStpLogic")
    public StpLogic dispatcherStpLogic() {
        return new StpLogic(AuthLoginType.DISPATCHER);
    }

    @Bean("adminStpLogic")
    public StpLogic adminStpLogic() {
        return new StpLogic(AuthLoginType.ADMIN);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
