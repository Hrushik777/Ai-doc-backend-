package com.example.ai_doc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * Runs without a database.
 *
 * <p>Nothing in the processing pipeline reads persisted state: {@code /process},
 * {@code /process/explain} and {@code /process/batch} take the uploaded file, produce a
 * workbook, and keep nothing. The only writer was the upload endpoint, whose rows were never
 * read back, so the datasource is excluded rather than provisioned.
 *
 * <p>All three exclusions are needed. Dropping only the datasource leaves Hibernate trying to
 * build an {@code EntityManagerFactory} against a {@code DataSource} that no longer exists,
 * which fails the context at startup. The remaining JDBC auto-configurations are conditional
 * on a {@code DataSource} bean and stand down on their own.
 *
 * <p>To restore persistence: delete this exclude list, uncomment the datasource and JPA
 * properties in {@code application.properties}, {@code @Service} on {@code DocumentService},
 * and the upload endpoint in {@code DocumentController}.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class
})
public class AiDocApplication {

	public static void main(String[] args) {

        SpringApplication.run(AiDocApplication.class, args);
	}

}
