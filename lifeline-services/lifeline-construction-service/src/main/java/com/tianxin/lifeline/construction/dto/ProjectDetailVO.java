package com.tianxin.lifeline.construction.dto;

import com.tianxin.lifeline.construction.entity.ConstructionMedia;
import com.tianxin.lifeline.construction.entity.ConstructionProcess;
import com.tianxin.lifeline.construction.entity.ConstructionViolation;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 施工项目详情聚合视图（项目 + 工序 + 违规 + 影像）。
 */
@Data
public class ProjectDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目视图对象（含坐标与状态文本） */
    private ProjectVO project;

    /** 工序清单（含质检记录） */
    private List<ConstructionProcess> processes;

    /** 违规记录清单 */
    private List<ConstructionViolation> violations;

    /** 影像清单（对象键） */
    private List<ConstructionMedia> media;
}
