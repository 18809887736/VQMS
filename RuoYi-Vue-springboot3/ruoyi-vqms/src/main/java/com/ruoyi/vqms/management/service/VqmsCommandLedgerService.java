package com.ruoyi.vqms.management.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.management.domain.VqmsCommandLedger;
import com.ruoyi.vqms.management.mapper.VqmsCommandLedgerMapper;
import com.ruoyi.vqms.source.model.WarnInfo;
import com.ruoyi.vqms.source.reader.SourceReader;

/**
 * AVC 指令流水账抓取 Service（确定轨 D8，v5.0 §6.2.6 / §8.6）。
 *
 * <p>经 source 层读外部源 warn_info（筛 warn_type=5）→ 原始字段摘录落账
 * {@code vqms_command_ledger}（存储切分铁律唯一有界例外表）。幂等：insert ignore +
 * uk_cmd（生成列 NULL 归一），重复抓取 DB 层静默跳过、返回新增行数——无应用层
 * check-then-insert 竞态。</p>
 *
 * <p>只记不判（§8.6 搁置期计数契约）：本类不做任何解码/判定；undecodable 计数等
 * 随搁置轨解封后从本账原文重算。抓取编排（定时触发）随 S4 Quartz，不在本类。
 * 注意 ingest 不得加 @Transactional——readWarn 经 @DataSource(SLAVE) 切外部源，
 * 事务会把连接钉在主库导致查不到 warn_info。</p>
 */
@Service
public class VqmsCommandLedgerService
{
    private static final Logger log = LoggerFactory.getLogger(VqmsCommandLedgerService.class);

    /** 电压指令 = 遥调（v5.0 §8.1） */
    private static final long WARN_TYPE_COMMAND = 5L;

    @Autowired
    private SourceReader sourceReader;

    @Autowired
    private VqmsCommandLedgerMapper ledgerMapper;

    /**
     * 抓取 [startTime, endTime] 内的电压指令摘录入账（幂等，可重复调用）。
     *
     * @return 实际新增行数（重复跳过不计）
     */
    public int ingest(String startTime, String endTime)
    {
        List<WarnInfo> commands = sourceReader.readWarn(startTime, endTime, WARN_TYPE_COMMAND);
        if (commands.isEmpty())
        {
            log.info("指令流水账抓取 [{}, {}]: 外部源 0 条指令", startTime, endTime);
            return 0;
        }
        List<VqmsCommandLedger> rows = new ArrayList<>(commands.size());
        for (WarnInfo cmd : commands)
        {
            rows.add(toLedger(cmd));
        }
        int inserted = ledgerMapper.insertIgnoreBatch(rows);
        log.info("指令流水账抓取 [{}, {}]: 外部源 {} 条, 新增 {} 条, 重复跳过 {} 条",
                startTime, endTime, commands.size(), inserted, commands.size() - inserted);
        return inserted;
    }

    /** 原文字段一一摘录，不做任何解码；fetched_at 走 DB default */
    private VqmsCommandLedger toLedger(WarnInfo cmd)
    {
        VqmsCommandLedger row = new VqmsCommandLedger();
        row.setWarnTime(cmd.getWarnTime());
        row.setMillisecond(cmd.getMillisecond());
        row.setWarnType(cmd.getWarnType());
        row.setObjNum(cmd.getObjNum());
        row.setWarnContent(cmd.getWarnContent());
        return row;
    }
}
