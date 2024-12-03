/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.configuration.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneConfigurationManager;
import java.util.Map;
import org.sql2o.Sql2o;
import org.springframework.util.StringUtils;

/**
 *
 * @author Top
 */
public class FonteRibaltoneConnection extends RibaltoneConfigurationManager{

    @Override
    public Sql2o getConnection(Map<String, String> configuration) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(configuration.get("driver"));
        if (StringUtils.hasText(configuration.get("url")) && StringUtils.hasText(configuration.get("port"))) {
            hikariConfig.setJdbcUrl(((String) configuration.get("url")).replaceAll("(jdbc:oracle:thin:@)(.+):(\\d+)\\/(.+)",
                    String.format("$1%s:%s/$4", configuration.get("url"), configuration.get("port"))));
           
        } else {
            hikariConfig.setJdbcUrl((String) configuration.get("url"));
        }
        hikariConfig.setUsername((String) configuration.get("username"));
        hikariConfig.setPassword((String) configuration.get("password"));
        hikariConfig.setMinimumIdle(Integer.parseInt(configuration.get("sql2oMinIdleSize")));
        hikariConfig.setMaximumPoolSize(Integer.parseInt(configuration.get("sql2oMaxPoolSize")));
        hikariConfig.setConnectionTimeout(Integer.parseInt(configuration.get("sql2oConnectionTimeout")));
        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
        return new Sql2o(hikariDataSource);
    }

}
