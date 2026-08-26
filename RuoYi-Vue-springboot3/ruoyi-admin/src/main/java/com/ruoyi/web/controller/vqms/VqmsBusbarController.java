package com.ruoyi.web.controller.vqms;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.management.domain.VqmsBusbar;
import com.ruoyi.vqms.management.service.VqmsBusbarService;

/**
 * 母线支撑接口（v5.0 §10.1）：母线下拉，行带 vGrade，供电压等级级联过滤。
 * 2026-08-26 增：母线台账 CRUD（vqms_busbar 此前仅 DB 手工维护；现场数据接入后需页面维护）。
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

    /** 母线详情 */
    @PreAuthorize("@ss.hasPermi('vqms:vqms_busbar:query')")
    @GetMapping(value = "/{busbarNum}")
    public AjaxResult getInfo(@PathVariable Long busbarNum)
    {
        return success(busbarService.selectByBusbarNum(busbarNum));
    }

    /** 新增母线（group_num 逻辑 FK 校验在 Service 层） */
    @PreAuthorize("@ss.hasPermi('vqms:vqms_busbar:add')")
    @Log(title = "母线台账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsBusbar busbar)
    {
        try
        {
            return toAjax(busbarService.insert(busbar));
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            throw new ServiceException(e.getMessage());
        }
    }

    /** 修改母线（group_num 逻辑 FK 校验在 Service 层） */
    @PreAuthorize("@ss.hasPermi('vqms:vqms_busbar:edit')")
    @Log(title = "母线台账", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsBusbar busbar)
    {
        try
        {
            return toAjax(busbarService.update(busbar));
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            throw new ServiceException(e.getMessage());
        }
    }

    /** 删除母线 */
    @PreAuthorize("@ss.hasPermi('vqms:vqms_busbar:remove')")
    @Log(title = "母线台账", businessType = BusinessType.DELETE)
    @DeleteMapping("/{busbarNum}")
    public AjaxResult remove(@PathVariable Long busbarNum)
    {
        return toAjax(busbarService.deleteByBusbarNum(busbarNum));
    }
}
