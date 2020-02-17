package com.packet.config;

import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import redis.clients.jedis.JedisPoolConfig;

import javax.sql.DataSource;
import java.util.Properties;
@Configuration
@ComponentScan(value = "com.*",includeFilters = {@ComponentScan.Filter(type = FilterType.ANNOTATION,value = {Service.class})})
@EnableTransactionManagement
public class RootConfig implements TransactionManagementConfigurer {
    private DataSource dataSource;


    @Bean(name = "dataSource")
    public DataSource initDatasource() {
        if (dataSource != null){
            return dataSource;
        }
        Properties props = new Properties();
        props.setProperty("driverClassName","com.mysql.cj.jdbc.Driver");
        props.setProperty("url","jdbc:mysql://localhost:3306/red_packet?serverTimezone=GMT%2B8");
        props.setProperty("username","root");
        props.setProperty("password","root");
        props.setProperty(" maxActive","200");
        props.setProperty("maxIdle","20");
        props.setProperty("maxWait","30000");
        try {
            dataSource = BasicDataSourceFactory.createDataSource(props);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataSource;

    }
    @Override
    @Bean
    public TransactionManager annotationDrivenTransactionManager() {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(initDatasource());
        return transactionManager;
    }

    @Bean(name="sqlSessionFactory")
    public SqlSessionFactoryBean initSqlSessionFactory(){
        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(initDatasource());
        Resource resource = new ClassPathResource("mybatis/mybatis-config.xml");
        sqlSessionFactory.setConfigLocation(resource);
        return sqlSessionFactory;
    }

    @Bean
    public MapperScannerConfigurer initMapperScannerConfigurer(){
        MapperScannerConfigurer mapperScanner = new MapperScannerConfigurer();
        mapperScanner.setBasePackage("com.*");
        mapperScanner.setSqlSessionFactoryBeanName("sqlSessionFactory");
        mapperScanner.setAnnotationClass(Repository.class);
        return mapperScanner;
    }

    @Bean(name = "redisTemplate")
    public RedisTemplate initRedisTemplate(){
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(50);
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxWaitMillis(20000);

        JedisConnectionFactory jedisFactory = new JedisConnectionFactory(poolConfig);
        jedisFactory.setHostName("192.168.1.4");
        jedisFactory.setPort(6379);
        jedisFactory.setPassword("root");
        jedisFactory.afterPropertiesSet();
        RedisSerializer serializer = new JdkSerializationRedisSerializer();
        RedisSerializer serializerString = new StringRedisSerializer();

        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(jedisFactory);

        redisTemplate.setDefaultSerializer(serializerString);
        redisTemplate.setKeySerializer(serializerString);
        redisTemplate.setValueSerializer(serializerString);
        redisTemplate.setHashKeySerializer(serializerString);
        redisTemplate.setHashValueSerializer(serializerString);
        return redisTemplate;
    }
}
