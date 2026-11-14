package com.tianxin.lifeline.construction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.construction.config.MinioProperties;
import com.tianxin.lifeline.construction.dto.MediaUploadVO;
import com.tianxin.lifeline.construction.entity.ConstructionMedia;
import com.tianxin.lifeline.construction.entity.ConstructionProject;
import com.tianxin.lifeline.construction.mapper.ConstructionMediaMapper;
import com.tianxin.lifeline.construction.mapper.ConstructionProjectMapper;
import com.tianxin.lifeline.construction.service.ConstructionMediaService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 施工影像业务服务实现。
 *
 * <p>MinIO 流式上传：直接消费 MultipartFile 输入流经 putObject 写入
 * 对象存储（不落本地磁盘），construction_media 表只存对象键。
 * 影像桶不存在时首次上传自动创建（懒初始化）。
 */
@Slf4j
@Service
public class ConstructionMediaServiceImpl implements ConstructionMediaService {

    /** 对象键日期目录格式 */
    private static final DateTimeFormatter DATE_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** MinIO 客户端 */
    private final MinioClient minioClient;

    /** MinIO 配置属性 */
    private final MinioProperties minioProperties;

    /** 影像 Mapper */
    private final ConstructionMediaMapper mediaMapper;

    /** 项目 Mapper */
    private final ConstructionProjectMapper projectMapper;

    /** 影像桶懒初始化标记 */
    private final AtomicBoolean bucketReady = new AtomicBoolean(false);

    /**
     * 构造服务（构造器注入）。
     *
     * @param minioClient      MinIO 客户端
     * @param minioProperties  MinIO 配置属性
     * @param mediaMapper      影像 Mapper
     * @param projectMapper    项目 Mapper
     */
    public ConstructionMediaServiceImpl(MinioClient minioClient,
                                        MinioProperties minioProperties,
                                        ConstructionMediaMapper mediaMapper,
                                        ConstructionProjectMapper projectMapper) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.mediaMapper = mediaMapper;
        this.projectMapper = projectMapper;
    }

    /**
     * 查询项目下的影像清单（采集时间降序）。
     *
     * @param projectId 项目ID
     * @return 影像列表
     */
    @Override
    public List<ConstructionMedia> listByProject(Long projectId) {
        LambdaQueryWrapper<ConstructionMedia> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConstructionMedia::getProjectId, projectId)
                .orderByDesc(ConstructionMedia::getCaptureTime).orderByDesc(ConstructionMedia::getId);
        return mediaMapper.selectList(wrapper);
    }

    /**
     * 流式上传施工影像至 MinIO 并落影像记录（DB 只存对象键）。
     *
     * @param projectId 项目ID
     * @param file      影像文件
     * @param cameraId  摄像头ID（可空）
     * @return 上传结果（含对象键）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaUploadVO upload(Long projectId, MultipartFile file, String cameraId) {
        ConstructionProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.CONSTRUCTION_NOT_FOUND);
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "影像文件不能为空");
        }

        long mediaId = SnowflakeIdGenerator.getInstance().nextId();
        String mediaType = resolveMediaType(file.getContentType(), file.getOriginalFilename());
        String objectKey = buildObjectKey(projectId, mediaId, extensionOf(file.getOriginalFilename()));

        // 流式上传：直接消费请求输入流，不落本地磁盘
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(StringUtils.hasText(file.getContentType())
                            ? file.getContentType() : "application/octet-stream")
                    .build());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("施工影像MinIO上传失败: projectId={}, objectKey={}", projectId, objectKey, e);
            throw new BizException(ErrorCode.REMOTE_CALL_ERROR, "影像上传失败：" + e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        ConstructionMedia media = new ConstructionMedia();
        media.setId(mediaId);
        media.setProjectId(projectId);
        media.setMediaType(mediaType);
        media.setFileUrl(objectKey);
        media.setCaptureTime(now);
        media.setCameraId(cameraId);
        mediaMapper.insert(media);
        log.info("施工影像上传完成: projectId={}, mediaId={}, objectKey={}", projectId, mediaId, objectKey);

        MediaUploadVO vo = new MediaUploadVO();
        vo.setId(mediaId);
        vo.setProjectId(projectId);
        vo.setMediaType(mediaType);
        vo.setObjectKey(objectKey);
        vo.setCaptureTime(now);
        return vo;
    }

    /* ==================== 私有辅助方法 ==================== */

    /**
     * 影像桶懒初始化：不存在时自动创建（并发安全，仅首次执行）。
     */
    private void ensureBucket() throws Exception {
        if (bucketReady.get()) {
            return;
        }
        synchronized (bucketReady) {
            if (bucketReady.get()) {
                return;
            }
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
                log.info("MinIO 影像桶自动创建: bucket={}", minioProperties.getBucket());
            }
            bucketReady.set(true);
        }
    }

    /**
     * 判定影像类型：Content-Type 为 video/* 记 video，其余记 image；
     * Content-Type 缺失时按扩展名兜底。
     *
     * @param contentType  Content-Type
     * @param originalName 原始文件名
     * @return image / video
     */
    private String resolveMediaType(String contentType, String originalName) {
        if (StringUtils.hasText(contentType)) {
            return contentType.toLowerCase().startsWith("video/") ? "video" : "image";
        }
        String ext = extensionOf(originalName).toLowerCase();
        return (".mp4".equals(ext) || ".avi".equals(ext) || ".mov".equals(ext) || ".flv".equals(ext))
                ? "video" : "image";
    }

    /**
     * 构建对象键：construction/{projectId}/{yyyyMMdd}/{mediaId}{ext}。
     *
     * @param projectId 项目ID
     * @param mediaId   影像记录ID
     * @param extension 文件扩展名（含点，可为空串）
     * @return MinIO 对象键
     */
    private String buildObjectKey(Long projectId, long mediaId, String extension) {
        return "construction/" + projectId + "/" + LocalDate.now().format(DATE_DIR_FORMAT)
                + "/" + mediaId + extension;
    }

    /**
     * 提取文件扩展名（含点），非法字符忽略返回空串。
     *
     * @param originalName 原始文件名
     * @return 扩展名（如 .jpg），无扩展名返回空串
     */
    private String extensionOf(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        String ext = originalName.substring(dot).toLowerCase();
        // 仅保留常规影像扩展名，防止对象键注入异常字符
        return ext.matches("\\.[a-z0-9]{1,8}") ? ext : "";
    }
}
