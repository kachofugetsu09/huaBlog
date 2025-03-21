package site.hnfy258.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.VO.LinkVo;
import site.hnfy258.annotation.SystemLog;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.service.LinkService;

import java.util.List;

@RestController
@RequestMapping("/link")
@Api(tags ="友链管理")

public class LinkController {
    @Autowired
    private LinkService linkService;

    @ApiOperation(value = "获取所有友链", notes = "获取所有友链")
    @GetMapping("/getAllLink")
    @SystemLog(bussinessName = "获取友链信息")
    public ResponseResult<List<LinkVo>> getAllLink(){
        List<LinkVo> linkVoList = linkService.getAllLink();
        return ResponseResult.okResult(linkVoList);

    }

}
