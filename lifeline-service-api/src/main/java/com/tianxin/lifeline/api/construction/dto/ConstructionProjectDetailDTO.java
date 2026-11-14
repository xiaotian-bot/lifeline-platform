package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 施工项目详情聚合 DTO（服务间共享）。
 */
@Data
public class ConstructionProjectDetailDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目视图对象 */
    private ConstructionProjectDTO project;

    /** 工序清单 */
    private List<ConstructionProcessDTO> processes;

    /** 违规记录清单 */
    private List<ConstructionViolationDTO> violations;

    /** 影像清单 */
    private List<ConstructionMediaDTO> media;
}
