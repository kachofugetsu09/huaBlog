package site.hnfy258.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.hnfy258.Exception.SystemException;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.enums.AppHttpCodeEnum;
import site.hnfy258.service.UploadService;
import site.hnfy258.utils.PathUtils;

import java.io.InputStream;

@Service("UploadService")
@Data
@ConfigurationProperties(prefix = "oss")
public class UploadServiceImpl implements UploadService {
    private String accessKeyId;       // 阿里云 AccessKeyId
    private String accessKeySecret;      // 阿里云 AccessKeySecret
    private String bucket;         // 阿里云 Bucket 名称
    private String endpoint;       // 阿里云 OSS Endpoint

    @Override
    public ResponseResult uploadImg(MultipartFile img) {
        // 判断文件类型
        String originalFilename = img.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".png")&&!originalFilename.endsWith(".jpg"))) {
            throw new SystemException(AppHttpCodeEnum.FILE_TYPE_ERROR);
        }

        // 生成文件路径
        String filePath = PathUtils.generateFilePath(originalFilename);

        // 上传文件到阿里云 OSS
        String url = uploadOss(img, filePath); // 例如：http://bucket.endpoint/2025/03/15/filename.png
        return ResponseResult.okResult(url);
    }

    private String uploadOss(MultipartFile imgFile, String filePath) {
        // 创建 OSS 客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try (InputStream inputStream = imgFile.getInputStream()) {
            // 创建 PutObjectRequest
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, filePath, inputStream);

            // 上传文件
            ossClient.putObject(putObjectRequest);

            // 返回文件的访问 URL
            return "https://" + bucket + "." + endpoint + "/" + filePath;
        } catch (Exception ex) {
            throw new SystemException(AppHttpCodeEnum.FILE_UPLOAD_ERROR);
        } finally {
            // 关闭 OSS 客户端
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
