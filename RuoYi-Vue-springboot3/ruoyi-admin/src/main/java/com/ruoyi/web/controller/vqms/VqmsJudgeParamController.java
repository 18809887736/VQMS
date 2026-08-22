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
import com.ruoyi.vqms.management.domain.VqmsJudgeParam;
import com.ruoyi.vqms.management.service.VqmsJudgeParamService;

/**
 * 判定整定参数管理（v5.0 §6.2.5 / §10.1，D7）——值域/锁定双层校验（Service 友好报错 + DB CHECK 兜底），
 * 改值即刷 Redis 缓存（vqms:judgeParam:{key}），判定侧下一次读取生效。
 */
@RestController
@RequestMapping("/vqms/judgeParam")
public class VqmsJudgeParamController extends BaseController
{
    @Autowired
    private VqmsJudgeParamService judgeParamService;

    @PreAuthorize("@ss.hasPermi('vqms:judgeparam:list')")
    @GetMapping("/list")
    public TableDataInfo list()
    {
        List<VqmsJudgeParam> list = judgeParamService.selectList();
        TableDataInfo data = new TableDataInfo();
        data.setCode(200);
        data.setRows(list);
        data.setTotal(list.size());
        return data;
    }

    @PreAuthorize("@ss.hasPermi('vqms:judgeparam:add')")
    @Log(title = "判定整定参数", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsJudgeParam param)
    {
        return toAjax(judgeParamService.insert(param));
    }

    @PreAuthorize("@ss.hasPermi('vqms:judgeparam:edit')")
    @Log(title = "判定整定参数", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsJudgeParam param)
    {
        return toAjax(judgeParamService.update(param));
    }

    @PreAuthorize("@ss.hasPermi('vqms:judgeparam:remove')")
    @Log(title = "判定整定参数", businessType = BusinessType.DELETE)
    @DeleteMapping("/{paramIds}")
    public AjaxResult remove(@PathVariable Long[] paramIds)
    {
        int rows = 0;
        for (Long id : paramIds)
        {
            rows += judgeParamService.deleteById(id);
        }
        return toAjax(rows);
    }
}
