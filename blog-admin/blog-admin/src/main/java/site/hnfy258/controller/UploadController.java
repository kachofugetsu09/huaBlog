package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import site.hnfy258.Exception.SystemException;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.enums.AppHttpCodeEnum;
import site.hnfy258.service.UploadService;

@RestController


public class UploadController {
    @Autowired
    private UploadService uploadService;
    @PostMapping("/upload")
    public ResponseResult uploadImg(@RequestParam("img") MultipartFile img){
        try{
           return uploadService.uploadImg(img);
        }
        catch(Exception e){
            e.printStackTrace();
            throw new SystemException(AppHttpCodeEnum.FILE_UPLOAD_ERROR);
        }
    }

}
