package rinha.repositories;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatasorceFactory {

    public static DataSource createDataSourceHikari() {

        HikariDataSource source = null;
        int count = 0;
        while (count <= 10) {
            try {
                count++;
                source = new HikariDataSource(new HikariConfig("/hikari.properties"));
                break;
            } catch (Exception e) {
                log.error("Start Datasource " + count + ": " + e.getMessage());
                try {
                    Thread.sleep(Duration.ofSeconds(1));
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        if (source == null) {
            throw new RuntimeException("Não foi possível criar o data source");
        }

//        ThreadFactory factory = Thread.ofVirtual().factory();
//        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(0, factory);
//        
//         source.setScheduledExecutor(Executors.newVirtualThreadPerTaskExecutor());
        return source;
    }

}
