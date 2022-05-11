//package it.bologna.ausl.internauta.utils.firma.remota.configuration;
//
//import java.util.HashMap;
//import java.util.Map;
//import javax.sql.DataSource;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.PropertySource;
//import org.springframework.core.env.Environment;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.jdbc.datasource.DriverManagerDataSource;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
//import org.springframework.transaction.PlatformTransactionManager;
//
//@Configuration
//@PropertySource({ "classpath:application.properties" })
//@EnableJpaRepositories(
//    basePackages = "it.bologna.ausl.internauta.utils.firma.repositories", 
//    entityManagerFactoryRef = "firmaEntityManager", 
//    transactionManagerRef = "firmaTransactionManager"
//)
//public class PersistenceFirmaConfigurationBak {
//    @Autowired
//    private Environment env;
//    
//    @Bean
//    //@Primary
//    public LocalContainerEntityManagerFactoryBean firmaEntityManager() {
//        LocalContainerEntityManagerFactoryBean em
//          = new LocalContainerEntityManagerFactoryBean();
//        em.setDataSource(firmaDataSource());
//        em.setPackagesToScan(
//          new String[] { "it.bologna.ausl.internauta.utils.firma.model" });
//
//        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
//        em.setJpaVendorAdapter(vendorAdapter);
//        Map<String, Object> properties = new HashMap<>();
////        properties.put("hibernate.hbm2ddl.auto", env.getProperty("hibernate.hbm2ddl.auto"));
////        properties.put("hibernate.dialect", env.getProperty("hibernate.dialect"));
//        properties.put("spring.jpa.generate-ddl", env.getProperty("spring.jpa.generate-ddl"));
//        properties.put("spring.jpa.hibernate.ddl-auto", env.getProperty("spring.jpa.hibernate.ddl-auto"));
//        properties.put("spring.jpa.show-sql", env.getProperty("spring.jpa.show-sql"));
//        properties.put("spring.jpa.properties.hibernate.format_sql", env.getProperty("spring.jpa.properties.hibernate.format_sql"));
//        properties.put("spring.jpa.properties.hibernate.jdbc.fetch_size", env.getProperty("spring.jpa.properties.hibernate.jdbc.fetch_size"));
//        properties.put("spring.jpa.properties.hibernate.dialect", env.getProperty("spring.jpa.properties.hibernate.dialect"));
//        properties.put("spring.jpa.properties.hibernate.proc.param_null_passing", env.getProperty("spring.jpa.properties.hibernate.proc.param_null_passing"));
//        em.setJpaPropertyMap(properties);
//
//        return em;
//    }
//
////    @Primary
//    @Bean
//    public DataSource firmaDataSource() {
//        DriverManagerDataSource dataSource = new DriverManagerDataSource();
//        dataSource.setDriverClassName(env.getProperty("firma.spring.datasource.driver-class-name"));
//        dataSource.setUrl(env.getProperty("firma.spring.datasource.url"));
//        dataSource.setUsername(env.getProperty("firma.spring.datasource.hikari.username"));
//        dataSource.setPassword(env.getProperty("firma.spring.datasource.hikari.password"));
//
//        return dataSource;
//    }
//
////    @Primary
//    @Bean
//    public PlatformTransactionManager firmaTransactionManager() {
//        JpaTransactionManager transactionManager = new JpaTransactionManager();
//        transactionManager.setEntityManagerFactory(firmaEntityManager().getObject());
//        return transactionManager;
//    }
//}