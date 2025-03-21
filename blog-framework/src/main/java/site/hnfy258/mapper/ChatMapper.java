package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mysql.cj.protocol.Message;
import org.apache.ibatis.annotations.Mapper;
import site.hnfy258.entity.Article;
import site.hnfy258.entity.Messages;

@Mapper

public interface ChatMapper extends BaseMapper<Messages>{

}
