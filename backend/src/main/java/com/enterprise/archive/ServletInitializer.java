package com.enterprise.archive;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 外部 Tomcat 部署入口。
 * 打成 war 放到 Tomcat webapps 下时，由容器通过该初始化器启动 Spring 容器；
 * 以 java -jar 方式运行 war 时仍走 {@link ArchiveApplication#main(String[])}。
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ArchiveApplication.class);
    }
}
