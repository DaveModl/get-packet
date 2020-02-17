package com.packet.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;

public class WebAppInitiallizer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{
                RootConfig.class
        };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{
                WebConfig.class
        };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{
                "*.do"
        };
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        String filePath = "D:\\get-packet\\src\\main\\resources\\upload";
        Long singleMax = (long) (5*Math.pow(2,20));
        Long totalMax = (long) (10*Math.pow(2,20));
        registration.setMultipartConfig(new MultipartConfigElement(filePath,singleMax,totalMax,0));
    }
}
