package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.Sessions;
import site.hnfy258.service.SessionsService;
import site.hnfy258.mapper.SessionMapper;

/**
 * (Sessions)表服务实现类
 *
 * @author makejava
 * @since 2025-02-26 21:08:29
 */
@Service("sessionsService")
public class SessionsServiceImpl extends ServiceImpl<SessionMapper, Sessions> implements SessionsService {

}

