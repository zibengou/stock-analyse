package com.stock.analyseservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    private ApiInfo initApiInfo() {
        ApiInfo apiInfo = new ApiInfo("Stock API",//大标题
                initContextInfo(),//简单的描述
                "1.0.0",//版本
                "服务条款",
                "xiaotong.shi",//作者
                "jowoiot.gitee",//链接显示文字
                "https://gitee.com/jowoiot_zibengou/events"//网站链接
        );

        return apiInfo;
    }

    private String initContextInfo() {
        StringBuffer sb = new StringBuffer();
        sb.append("股票服务 接口文档(V1)");
        return sb.toString();
    }


    @Bean
    public Docket restfulApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("RestfulApi")
                .genericModelSubstitutes(ResponseEntity.class)
                .useDefaultResponseMessages(true)
                .forCodeGeneration(false)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.stock.analyseservice.controller"))
                .paths(PathSelectors.regex("/.*"))
                .build()
                .apiInfo(initApiInfo());
    }

}
