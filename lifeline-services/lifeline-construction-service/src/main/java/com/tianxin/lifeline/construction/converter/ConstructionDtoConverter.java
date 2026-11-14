package com.tianxin.lifeline.construction.converter;

import com.tianxin.lifeline.construction.constant.ProjectStatus;
import com.tianxin.lifeline.construction.dto.ProjectVO;
import com.tianxin.lifeline.construction.entity.ConstructionProject;

/**
 * 施工域 DTO 转换器（实体 ↔ 视图对象）。
 */
public final class ConstructionDtoConverter {

    private ConstructionDtoConverter() {
    }

    /**
     * 项目实体转视图对象（回填状态中文文本）。
     *
     * @param project 项目实体
     * @return 项目视图对象，入参为 null 返回 null
     */
    public static ProjectVO toProjectVO(ConstructionProject project) {
        if (project == null) {
            return null;
        }
        ProjectVO vo = new ProjectVO();
        vo.setId(project.getId());
        vo.setProjectName(project.getProjectName());
        vo.setLocation(project.getLocation());
        vo.setContractor(project.getContractor());
        vo.setSupervisor(project.getSupervisor());
        vo.setStartDate(project.getStartDate());
        vo.setEndDate(project.getEndDate());
        vo.setStatus(project.getStatus());
        vo.setCreatedAt(project.getCreatedAt());
        vo.setUpdatedAt(project.getUpdatedAt());
        fillStatusText(vo);
        return vo;
    }

    /**
     * 回填项目状态中文文本。
     *
     * @param vo 项目视图对象
     */
    public static void fillStatusText(ProjectVO vo) {
        if (vo != null) {
            vo.setStatusText(ProjectStatus.toText(vo.getStatus()));
        }
    }
}
