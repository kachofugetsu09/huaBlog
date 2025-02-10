package site.hnfy258;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("site.hnfy258.mapper")
@EnableScheduling
public class HuaYueBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(HuaYueBlogApplication.class, args);
    }
}
