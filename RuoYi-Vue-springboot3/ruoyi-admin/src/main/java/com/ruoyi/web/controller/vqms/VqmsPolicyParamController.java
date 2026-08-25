package com.ruoyi.web.controller.vqms;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.vqms.management.domain.VqmsPolicyParam;
import com.ruoyi.vqms.management.service.VqmsPolicyParamService;

/**
 * 策略参数页（v5.0 §8.7，2026-08-22 Leo 拍板设计；S5 UI 交付）。
 *
 * <p>唯一写路径 = apply（套别整组 upsert 四约定键 + 写穿缓存 + @Log 留痕）；
 * 不开放逐行 add/edit/remove——杜绝绕过预设写出脏组合。选套值留空直至政策拍板，
 * 权限默认仅授管理员。</p>
 */
@RestController
@RequestMapping("/vqms/policyParam")
public class VqmsPolicyParamController extends BaseController
{
    @Autowired
    private VqmsPolicyParamService policyParamService;

    /** 当前四键原值行（只读查看） */
    @PreAuthorize("@ss.hasPermi('vqms:policyparam:list')")
    @GetMapping("/list")
    public TableDataInfo list()
    {
        List<VqmsPolicyParam> list = policyParamService.selectList();
        TableDataInfo data = new TableDataInfo();
        data.setCode(200);
        data.setRows(list);
        data.setTotal(list.size());
        return data;
    }

    /** 页面三态状态：未选套 / 已选套（含自定义阈值标注） */
    @PreAuthorize("@ss.hasPermi('vqms:policyparam:list')")
    @GetMapping("/state")
    public AjaxResult state()
    {
        return AjaxResult.success(policyParamService.currentState());
    }

    /**
     * 选套应用：{@code {"presetCode":"YI","thresholdPct":50}}——thresholdPct 可空（用预设默认），
     * 对无阈值预设提供即拒（Service 层校验）。
     */
    @PreAuthorize("@ss.hasPermi('vqms:policyparam:apply')")
    @Log(title = "策略参数选套", businessType = BusinessType.UPDATE)
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody Map<String, Object> body)
    {
        String presetCode = String.valueOf(body.get("presetCode"));
        Integer thresholdPct = body.get("thresholdPct") == null ? null
                : Integer.valueOf(String.valueOf(body.get("thresholdPct")));
        return AjaxResult.success(policyParamService.applyPreset(presetCode, thresholdPct, getUsername()));
    }

    /**
     * 戊·自由组合应用（策略文档 §3.3，2026-08-25）：
     * {@code {"rules":["A1 -> EXCLUDE_REPORTED","(A2 & !A3) -> PEND_MARKED"],"thresholdPct":50}}
     * ——规则行有序、首中即断；thresholdPct 可空（默认 50）。校验 fail-fast，整体拒绝时原生效策略不变。
     */
    @SuppressWarnings("unchecked")
    @PreAuthorize("@ss.hasPermi('vqms:policyparam:apply')")
    @Log(title = "策略自由组合应用", businessType = BusinessType.UPDATE)
    @PostMapping("/applyFreeform")
    public AjaxResult applyFreeform(@RequestBody Map<String, Object> body)
    {
        Object rulesObj = body.get("rules");
        if (!(rulesObj instanceof List))
        {
            return error("rules 须为规则行字符串数组");
        }
        Integer thresholdPct = body.get("thresholdPct") == null ? null
                : Integer.valueOf(String.valueOf(body.get("thresholdPct")));
        return AjaxResult.success(
                policyParamService.applyFreeform((List<String>) rulesObj, thresholdPct, getUsername()));
    }
}
