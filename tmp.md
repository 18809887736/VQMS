MySQL 5.7  root  syth7777  为我查看下数据

Listed MySQL databases on 10.0.0.35
连上了，有一个业务库 qheatavchisdb。看下里面的表。


Ran 4 commands
数据库情况汇总（库：qheatavchisdb，共 7 张表）:

表	行数	内容
yc_curve	11,892	遥测曲线数据
his_curve_sv	8,225	历史曲线（SV)
his_curve	7,520	历史曲线
adjust_info	1,088	调节记录
warn_info	820	告警记录
yc_history	792	遥测历史
ori_code	0	空表
数据时间范围：

实时数据活着: yc_history 从 2026-07-28 14:00 起持续写入，最新一条是今天（07-30)15:15，即系统正常运行中。
最新告警(07-28 14:47): "220KV东母线远方/就地分"、"检查AVC远方就地状态闭锁"、"远方就地总分"——是 AVC 系统重启（07-28 14:27 MySQL 启动时间吻合）时的状态切换告警，属正常。
07-28 14:28 还有两条 "AVC 调节闭锁"、"数据错误闭锁合"，也是启动瞬间产生的。
需要我导出某张表、查某个时间段的曲线，还是看别的？

请 将 his_curve_sv	8,225	历史曲线（SV)
his_curve	7,520	历史曲线   表结构给我  以及部分 数据