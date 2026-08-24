package com.ruoyi.vqms.statistics;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S1 L0：解码差分——Python decode.py（avc-data-gen 参考实现）vs Java VTargetDecoder，
 * 同一批 warn_info 原文逐条比对（测试方案 §5.0 差分②）。
 *
 * <p>fixture 由 {@code tools/avc-data-gen/verify/export_decode_fixture.py} 离线生成
 * （26 场景 SQL 中 19 调节场景的 20 条指令原文 + yc4002 阶跃保持实时电压），
 * 断言：Python 解码成功 ⟺ Java classify 通过；双方均成功时数值逐条相等。</p>
 */
class DecodeDiffFixtureTest
{
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fixture() throws Exception
    {
        try (var in = DecodeDiffFixtureTest.class.getResourceAsStream("/vqms/s1_decode_fixture.json"))
        {
            assertNotNull(in, "差分 fixture 缺失——先跑 tools/avc-data-gen/verify/export_decode_fixture.py");
            return JSON.parseObject(new String(in.readAllBytes(), StandardCharsets.UTF_8), List.class);
        }
    }

    @Test
    void pythonAndJava_agreeOnEveryCommand() throws Exception
    {
        List<Map<String, Object>> entries = fixture();
        assertEquals(20, entries.size(), "19 调节场景应含 20 条指令（S17 双指令）");

        int decoded = 0;
        for (Map<String, Object> e : entries)
        {
            AvcCommand cmd = new AvcCommand(
                    "t", "0", 0L, (String) e.get("warn_content"),
                    e.get("realtime_kv") == null ? null
                            : ((Number) e.get("realtime_kv")).doubleValue());
            Double py = e.get("py_v_target") == null ? null
                    : ((Number) e.get("py_v_target")).doubleValue();

            Optional<DecodeFailureReason> reason = VTargetDecoder.classify(cmd);
            if (py == null)
            {
                assertTrue(reason.isPresent(), e.get("scenario") + " Python 解码失败 → Java 亦须判失败");
            }
            else
            {
                assertTrue(reason.isEmpty(), e.get("scenario") + " Python 解码成功(" + py + ") → Java 亦须通过，实得 " + reason);
                double java = VTargetDecoder.decode(cmd);
                assertEquals(py, java, 1e-9, e.get("scenario") + " 差分数值须逐条相等");
                decoded++;
            }
        }
        assertTrue(decoded >= 17, "成功解码样本应 ≥17（20 − S11/S12 两类失败）: " + decoded);
    }
}
