package site.hnfy258;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(exclude={com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration.class},scanBasePackages = {"site.hnfy258"})
@MapperScan("site.hnfy258.mapper")
@EnableScheduling
@EnableSwagger2
public class HuaYueBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(HuaYueBlogApplication.class, args);
    }
}
