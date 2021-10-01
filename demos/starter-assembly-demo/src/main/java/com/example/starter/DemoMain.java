package com.example.starter;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 *
 */
public class DemoMain {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DemoMain.class);

    private static String[] fn = null;

    // 初始化spring文档
    private static void contextInitialized() {
        fn = new String[] {"applicationContext.xml"};
    }

    /**
     * @param args
     *
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        contextInitialized();
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(fn);

        Properties properties = PropertiesLoaderUtils.loadAllProperties("demo.properties");
        LOGGER.info(properties.toString());

        for (int i = 0; i < 1000000; ++i) {
            LOGGER.info(String.valueOf(i));

            Thread.sleep(500);
        }

        System.exit(0);
    }
}
