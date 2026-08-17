# git 历史密码擦除方案（10.0.0.35 root 泄露）

> 起草 2026-08-17。**本文档不含任何真实密码**——执行时涉及密码的值由 Leo 现场填入临时文件，该文件绝不进仓库。
> 性质：破坏性操作（重写全部历史 + force push），**须 Leo 明确授权后执行**（项目规划 v4.1 §14-12）。

## 1. 背景与现状（2026-08-17 核实）

- 仓库 `github.com/18809887736/VQMS` 当前为 **PUBLIC**，任何人都可无认证 clone 全历史。
- 泄露点：commit `26a89a9`（2026-07-30，"123"）引入的**仓库根** `tmp.md` 第 1 行含 **10.0.0.35（鸡西 QCzt AVC 盒子，已排除出 VQMS 范围）的 MySQL root 明文密码**。后续版本已抹除，但 git 历史仍可达。
- 全历史扫描结论：泄露凭证**仅此一条**。10.0.0.9 的凭证未进仓库（`.env.example` 全是 `change_me` 占位、`tools/avc-data-gen/verify/*.py` 走环境变量、`vqms-deploy` SKILL.md 只有 sed 替换命令）。

## 2. 顺序原则

1. **先轮换，后擦除**。密码一旦进入过公开仓库就视为已被获取，擦除历史不能撤销这件事；真正的止血是**轮换 10.0.0.35 的 MySQL root 密码**（需现场/对端协调；换之前确认 AVC 盒子自身的数据写入服务没用 root 连库）。轮换与本方案完全解耦、先行。
2. 历史擦除是防扩散与合规善后：避免未来任何 clone/缓存再取到该密码。

## 3. 执行前检查清单（Leo）

- [ ] 10.0.0.35 root 密码已轮换（或已与现场约期）
- [ ] GitHub 端暴露面：`gh api repos/18809887736/VQMS --jq .forks_count`——若已有 fork，见 §7
- [ ] 已做 **本地** mirror 备份（§4）；**备份绝不推回 GitHub 公开仓库**，否则等于没擦
- [ ] 无未推送的本地提交（擦除在 fresh clone 上做，本地工作区 `C:\work\VQMS` 不动）

## 4. 本地备份（不推远端）

```bash
git clone --mirror https://github.com/18809887736/VQMS.git VQMS-backup-20260817.git
```

留在本地/私有存储；擦除验证成功且运行一段时间后再销毁。

## 5. 擦除步骤（git filter-repo --replace-text，精准替换密码字符串）

只替换密码本身，保留 `tmp.md` 其余历史与全部提交信息。

```bash
pip install git-filter-repo

# 5.1 fresh clone 到独立目录（勿在 C:\work\VQMS 工作区内做）
git clone https://github.com/18809887736/VQMS.git VQMS-scrub
cd VQMS-scrub

# 5.2 在仓库外建替换规则文件——真实密码执行时手工填入，此文件不进任何仓库
#     literal: 前缀按字面量匹配，避免密码中的正则元字符干扰
echo 'literal:<10.0.0.35 的旧 root 密码>==>REMOVED-CREDENTIAL-2026-08' > /tmp/replacements.txt

# 5.3 执行擦除（--sensitive-data-removal 会额外清 reflog/旧引用，专为此场景设计；
#     若 git-filter-repo 版本过旧无此旗标，去掉后手动补：git reflog expire --expire=now --all && git gc --prune=now）
git filter-repo --replace-text /tmp/replacements.txt --sensitive-data-removal
# filter-repo 执行后会自动移除 origin（防误推），需重新添加：
git remote add origin https://github.com/18809887736/VQMS.git

# 5.4 校验（命令本身不含密码）
#   a. 替换标记能在历史 tmp.md 中找到：
git grep -l 'REMOVED-CREDENTIAL-2026-08' $(git rev-list --all)
#   b. 原提交 hash 已不存在（26a89a9 早于绝大多数提交，其后整条祖先链的 hash 都会变）：
git cat-file -t 26a89a9    # 期望报错 Not a valid object
#   c. 工作区当前文件不受影响（docs/tmp.md 现版本本就已抹除）
```

（`/tmp` 为 Git Bash 路径；Windows 下可换成 `%TEMP%` 对应目录。）

## 6. 推送（须 Leo 明确授权）

```bash
git push --force --all origin
git push --force --tags origin
```

- 推送后在 GitHub 网页确认旧 commit 链接（如 `26a89a9`）已 404。
- **所有旧 clone 作废**：其他机器/协作者必须重新 clone；不要在旧 clone 上 pull，会把旧历史带回来。本地工作区 `C:\work\VQMS` 同样重新 clone 或 `git fetch && git reset --hard origin/main`（先确认无本地未推提交）。

## 7. GitHub 端残留与兜底

force push 后 GitHub 服务器仍可能短期保留不可达对象（缓存视图、PR diff、fork）：

1. **无 fork / 无需保留 PR 时（本仓库目前基本单人开发）**：向 GitHub Support 提工单，请求对仓库执行 gc 清除不可达对象（附仓库 URL 与"密码已轮换"说明），一般数日内生效。
2. **最彻底兜底**（可接受丢失 issue/PR/stars 时）：删除仓库 → 用擦除后的历史重建同名仓库。URL 不变，但旧 PR/issue 链接失效。
3. **若已有 fork**：fork 端历史我们无法重写——此时唯一有效手段就是 §2 的轮换（可另联系 fork 所有者删除）。

无论选哪条：**轮换密码（§2）都是不可跳过的主手段**，擦除只是善后。

## 8. 完成后收尾

- [ ] 更新 `CLAUDE.md` Security 一节：明确泄露主机为 10.0.0.35（非 10.0.0.9），标注"已轮换 + 历史已擦除"的日期
- [ ] 删除 `/tmp/replacements.txt`；确认 mirror 备份的存放位置
- [ ] 可选：仓库如无公开必要，转 Private——`gh repo edit 18809887736/VQMS --visibility private`（Leo 定）
- [ ] 注意：重写后全部相关提交 hash 已变，此前文档/记忆中引用的旧 hash（如 `26a89a9`）仅存档意义
