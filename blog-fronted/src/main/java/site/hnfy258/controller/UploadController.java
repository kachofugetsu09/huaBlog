package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import site.hnfy258.annotation.SystemLog;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.service.UploadService;

@RestController
public class UploadController {
    @Autowired
    private UploadService uploadService;
    @SystemLog(bussinessName = "更新用户头像")
    @PostMapping("/upload")
    public ResponseResult uploadImg(MultipartFile img){
        return uploadService.uploadImg(img);
    }
}