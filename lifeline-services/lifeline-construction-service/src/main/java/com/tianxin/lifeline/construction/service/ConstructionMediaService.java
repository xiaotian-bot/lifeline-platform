package com.tianxin.lifeline.construction.service;

import com.tianxin.lifeline.construction.dto.MediaUploadVO;
import com.tianxin.lifeline.construction.entity.ConstructionMedia;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 施工影像业务服务。
 *
 * <p>影像经 MinIO 流式上传（不落本地磁盘），construction_media
 * 表只存对象键。
 */
public interface ConstructionMediaService {

    /**
     * 查询项目下的影像清单（采集时间降序）。
     *
     * @param projectId 项目ID
     * @return 影像列表
     */
    List<ConstructionMedia> listByProject(Long projectId);

    /**
     * 流式上传施工影像至 MinIO 并落影像记录（DB 只存对象键）。
     *
     * @param projectId 项目ID
     * @param file      影像文件
     * @param cameraId  摄像头ID（可空）
     * @return 上传结果（含对象键）
     */
    MediaUploadVO upload(Long projectId, MultipartFile file, String cameraId);
}
