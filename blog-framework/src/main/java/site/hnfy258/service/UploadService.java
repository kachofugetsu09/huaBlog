package site.hnfy258.service;

import org.springframework.web.multipart.MultipartFile;
import site.hnfy258.domain.ResponseResult;

public interface UploadService {
    ResponseResult uploadImg(MultipartFile img);
}
