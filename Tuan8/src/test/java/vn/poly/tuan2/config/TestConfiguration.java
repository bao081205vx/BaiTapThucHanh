package vn.poly.tuan2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@org.springframework.boot.test.context.TestConfiguration
@Profile("test")
public class TestConfiguration {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .addScript("classpath:schema.sql")
                .addScript("classpath:test-data.sql")
                .build();
    }
}
