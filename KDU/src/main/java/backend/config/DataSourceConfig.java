package backend.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import com.amazonaws.xray.sql.TracingDataSource;


import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;


    @Bean
    public DataSource dataSource() {
        // Replace with your actual database connection details
        DataSource originalDataSource = DataSourceBuilder.create()
                .url(jdbcUrl)  // Use your DB URL here
                .username(username)
                .password(password)
                .driverClassName(driverClassName)  // Replace with your DB driver class
                .build();

        // Wrap the original DataSource with X-Ray's TracingDataSource to trace SQL queries
        return new TracingDataSource(originalDataSource);
    }
}
