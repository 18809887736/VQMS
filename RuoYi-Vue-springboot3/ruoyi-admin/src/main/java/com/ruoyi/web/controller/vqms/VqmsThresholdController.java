package com.ruoyi.web.controller.vqms;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vqms.management.domain.VqmsBusbarThreshold;
import com.ruoyi.vqms.management.service.VqmsBusbarThresholdService;

/**
 * 母线电压阈值管理（v5.0 §10.1）——D2 Service 层逻辑 FK 校验（busbar_num 存在性 + 生效区间自动闭合）。
 */
@RestController
@RequestMapping("/vqms/threshold")
public class VqmsThresholdController extends BaseController
{
    @Autowired
    private VqmsBusbarThresholdService thresholdService;

    @PreAuthorize("@ss.hasPermi('vqms:threshold:list')")
    @GetMapping("/list")
    public TableDataInfo list(Long busbarNum, Integer vGrade)
    {
        startPage();
        List<VqmsBusbarThreshold> list = thresholdService.selectList(busbarNum, vGrade);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('vqms:threshold:query')")
    @GetMapping(value = "/{thresholdId}")
    public AjaxResult getInfo(@PathVariable Long thresholdId)
    {
        return success(thresholdService.selectById(thresholdId));
    }

    @PreAuthorize("@ss.hasPermi('vqms:threshold:add')")
    @Log(title = "母线电压阈值", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsBusbarThreshold threshold)
    {
        return toAjax(thresholdService.insert(threshold));
    }

    @PreAuthorize("@ss.hasPermi('vqms:threshold:edit')")
    @Log(title = "母线电压阈值", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsBusbarThreshold threshold)
    {
        return toAjax(thresholdService.update(threshold));
    }

    @PreAuthorize("@ss.hasPermi('vqms:threshold:remove')")
    @Log(title = "母线电压阈值", businessType = BusinessType.DELETE)
    @DeleteMapping("/{thresholdIds}")
    public AjaxResult remove(@PathVariable Long[] thresholdIds)
    {
        int rows = 0;
        for (Long id : thresholdIds)
        {
            rows += thresholdService.deleteById(id);
        }
        return toAjax(rows);
    }

    @PreAuthorize("@ss.hasPermi('vqms:threshold:export')")
    @Log(title = "母线电压阈值", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response)
    {
        List<VqmsBusbarThreshold> list = thresholdService.selectList(null, null);
        ExcelUtil<VqmsBusbarThreshold> util = new ExcelUtil<>(VqmsBusbarThreshold.class);
        util.exportExcel(response, list, "母线电压阈值");
    }
}
