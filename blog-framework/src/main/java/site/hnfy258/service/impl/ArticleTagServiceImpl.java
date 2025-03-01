package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import site.hnfy258.entity.ArticleTag;
import site.hnfy258.mapper.ArticleTagMapper;
import site.hnfy258.service.ArticleTagService;
import org.springframework.stereotype.Service;


@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagMapper, ArticleTag> implements ArticleTagService {
}
