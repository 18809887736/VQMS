package com.ruoyi.web.controller.vqms;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.vqms.management.domain.VqmsBusbar;
import com.ruoyi.vqms.management.service.VqmsBusbarService;

/**
 * 母线支撑接口（v5.0 §10.1）：母线下拉，行带 vGrade，供电压等级级联过滤。
 */
@RestController
@RequestMapping("/vqms/vqms_busbar")
public class VqmsBusbarController extends BaseController
{
    @Autowired
    private VqmsBusbarService busbarService;

    /** 母线全量（下拉数据源，不分页） */
    @PreAuthorize("@ss.hasPermi('vqms:vqms_busbar:list')")
    @GetMapping("/list")
    public TableDataInfo list()
    {
        List<VqmsBusbar> list = busbarService.selectList();
        TableDataInfo data = new TableDataInfo();
        data.setCode(200);
        data.setRows(list);
        data.setTotal(list.size());
        return data;
    }
}
