package site.hnfy258;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("site.hnfy258.mapper")

public class HuaYueBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(HuaYueBlogApplication.class, args);
    }
}
