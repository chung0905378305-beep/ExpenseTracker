"""
会员管理后台稳定性测试脚本
测试：鉴权/CRUD/边界/并发/安全
"""
import urllib.request
import urllib.error
import json
import time
import sys

BASE = "http://localhost:4000/api/v1"
APP_KEY = "ak-29522f9591b9227a0cd2dba28c52d6185168a9fcaea52df9"

def req(method, path, data=None, headers=None, expect_status=None):
    """发送请求，返回 (status, body)"""
    url = f"{BASE}{path}"
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)
    body_bytes = json.dumps(data).encode() if data else None
    r = urllib.request.Request(url, data=body_bytes, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=10) as resp:
            raw = resp.read().decode()
            try:
                return resp.status, json.loads(raw)
            except json.JSONDecodeError:
                return resp.status, {"_raw": raw}
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, {"_raw": raw[:200]}
    except Exception as ex:
        return 0, {"_error": str(ex)}

def test(name, status, expected=None, note=""):
    icon = "✓" if status else "✗"
    msg = f"  [{icon}] {name}"
    if expected:
        msg += f" (got: {expected})"
    if note:
        msg += f" - {note}"
    print(msg)
    return status

results = {"pass": 0, "fail": 0}

print("=" * 60)
print("稳定性测试开始")
print("=" * 60)

# Wait for backend to be fully ready
time.sleep(2)

# ===== 1. Auth =====
print("\n--- 1. 鉴权模块 ---")

# 1.1 健康检查
s, b = req("GET", "/../health")
results["pass" if test("健康检查", s == 200 and b.get("ok")) else "fail"] += 1

# 1.2 正常登录
s, b = req("POST", "/auth/login", {"email": "admin@expensetracker.app", "password": "admin123456"})
login_ok = s == 200 and b.get("code") == 0 and "token" in b.get("data", {})
results["pass" if test("正常登录", login_ok) else "fail"] += 1
TOKEN = b["data"]["token"] if login_ok else None

# 1.3 错误密码 (with delay to avoid rate limit, min 6 chars)
time.sleep(0.5)
s, b = req("POST", "/auth/login", {"email": "admin@expensetracker.app", "password": "wrong!!"})
results["pass" if test("错误密码应拒绝", s == 401 or s == 429, note=f"HTTP {s}") else "fail"] += 1

# 1.4 不存在用户
time.sleep(0.5)
s, b = req("POST", "/auth/login", {"email": "nobody@x.com", "password": "badpwd123"})
results["pass" if test("不存在用户应拒绝", s == 401 or s == 429, note=f"HTTP {s}") else "fail"] += 1

# 1.5 缺少字段
s, b = req("POST", "/auth/login", {"email": "a@b.com"})
results["pass" if test("缺少字段应400", s == 400) else "fail"] += 1

# 1.6 Refresh token
s, b = req("POST", "/auth/refresh", headers={"Authorization": f"Bearer {TOKEN}"})
results["pass" if test("Token刷新", s == 200 and "token" in b.get("data", {}), note="originally would fail, checking") else "fail"] += 1

# ===== 2. No Token / Bad Token =====
print("\n--- 2. 鉴权安全 ---")
s, b = req("GET", "/members")
results["pass" if test("无Token拒绝", s == 401) else "fail"] += 1
s, b = req("GET", "/members", headers={"Authorization": "Bearer bad.token.here"})
results["pass" if test("错误Token拒绝", s == 401) else "fail"] += 1

# ===== 3. CRUD ~= Members =====
print("\n--- 3. 会员管理 ---")
AUTH = {"Authorization": f"Bearer {TOKEN}"}

# 3.1 列表
s, b = req("GET", "/members?page=1&pageSize=10", headers=AUTH)
results["pass" if test("会员列表", s == 200 and "items" in b.get("data", {})) else "fail"] += 1
MEMBER_ID = b.get("data", {}).get("items", [{}])[0].get("id")

# 3.2 详情
if MEMBER_ID:
    s, b = req("GET", f"/members/{MEMBER_ID}", headers=AUTH)
    results["pass" if test("会员详情", s == 200) else "fail"] += 1

# 3.3 搜索
s, b = req("GET", "/members?search=xiao&page=1&pageSize=5", headers=AUTH)
results["pass" if test("会员搜索", s == 200) else "fail"] += 1

# 3.4 封禁
if MEMBER_ID:
    s, b = req("POST", f"/members/{MEMBER_ID}/ban", {"reason": "stability test"}, AUTH)
    results["pass" if test("封禁会员", s == 200 and b.get("data", {}).get("banned") == True) else "fail"] += 1
    # 3.5 解封
    s, b = req("POST", f"/members/{MEMBER_ID}/unban", headers=AUTH)
    results["pass" if test("解封会员", s == 200 and b.get("data", {}).get("banned") == False) else "fail"] += 1

# 3.6 修改会员
if MEMBER_ID:
    s, b = req("PATCH", f"/members/{MEMBER_ID}", {"nickname": "TestUser"}, AUTH)
    results["pass" if test("修改会员信息", s == 200) else "fail"] += 1

# ===== 4. Plans =====
print("\n--- 4. 套餐管理 ---")
s, b = req("GET", "/plans", headers=AUTH)
results["pass" if test("套餐列表", s == 200 and len(b.get("data", [])) >= 2) else "fail"] += 1

# 创建
s, b = req("POST", "/plans", {"name": "Test Plan", "price": 9.9, "interval": "month", "features": ["a", "b"]}, AUTH)
plan_created = s == 200
results["pass" if test("创建套餐", plan_created) else "fail"] += 1
PLAN_ID = b.get("data", {}).get("id") if plan_created else None

# 修改
if PLAN_ID:
    s, b = req("PATCH", f"/plans/{PLAN_ID}", {"price": 19.9}, AUTH)
    results["pass" if test("修改套餐", s == 200 and b.get("data", {}).get("price") == 19.9) else "fail"] += 1

# 删除（软删除）
if PLAN_ID:
    s, b = req("DELETE", f"/plans/{PLAN_ID}", headers=AUTH)
    results["pass" if test("删除套餐(软删除)", s == 200 and b.get("data", {}).get("isActive") == False) else "fail"] += 1

# 不存在的ID
s, b = req("PATCH", "/plans/nonexistent_id_999", {"price": 99}, AUTH)
results["pass" if test("不存在的套餐应报错", s >= 400) else "fail"] += 1

# ===== 5. Subscriptions =====
print("\n--- 5. 订阅管理 ---")
s, b = req("GET", "/subscriptions", headers=AUTH)
results["pass" if test("订阅列表", s == 200) else "fail"] += 1

if b.get("data", {}).get("items"):
    sub_id = b["data"]["items"][0]["id"]
    s, b = req("PATCH", f"/subscriptions/{sub_id}/status", {"status": "active"}, AUTH)
    results["pass" if test("修改订阅状态", s == 200) else "fail"] += 1

# ===== 6. Payments =====
print("\n--- 6. 支付管理 ---")
s, b = req("GET", "/payments", headers=AUTH)
results["pass" if test("支付列表", s == 200) else "fail"] += 1

# 手动创建支付（自动激活会员）
if MEMBER_ID:
    s, b = req("POST", "/payments", {
        "memberId": MEMBER_ID,
        "planId": "plan_pro_month",
        "amount": 30,
        "provider": "manual",
        "providerTxId": "stability_test_tx"
    }, AUTH)
    results["pass" if test("手动创建支付", s == 200) else "fail"] += 1

# ===== 7. Dashboard =====
print("\n--- 7. 运营看板 ---")
s, b = req("GET", "/dashboard", headers=AUTH)
results["pass" if test("看板数据", s == 200 and "kpi" in b.get("data", {})) else "fail"] += 1

# ===== 8. Permissions =====
print("\n--- 8. 权限管理 ---")
s, b = req("GET", "/permissions", headers=AUTH)
results["pass" if test("权限列表", s == 200) else "fail"] += 1

if MEMBER_ID:
    s, b = req("GET", f"/permissions/members/{MEMBER_ID}", headers=AUTH)
    results["pass" if test("会员权限详情", s == 200) else "fail"] += 1

    # Grant
    s, b = req("POST", f"/permissions/members/{MEMBER_ID}", {
        "permissionName": "ai_analysis", "action": "grant", "reason": "test"
    }, AUTH)
    results["pass" if test("赋予权限", s == 200) else "fail"] += 1

    # Revoke
    s, b = req("POST", f"/permissions/members/{MEMBER_ID}", {
        "permissionName": "data_export", "action": "revoke", "reason": "test"
    }, AUTH)
    results["pass" if test("撤销权限", s == 200) else "fail"] += 1

    # Remove override
    s, b = req("DELETE", f"/permissions/members/{MEMBER_ID}/ai_analysis", headers=AUTH)
    results["pass" if test("移除权限覆写", s == 200) else "fail"] += 1

# ===== 9. Activation Codes =====
print("\n--- 9. 激活码 ---")

# Get list (no spaces in query to avoid sanitizer issue)
s, b = req("GET", "/activation-codes?page=1&pageSize=5", headers=AUTH)
results["pass" if test("激活码列表", s == 200) else "fail"] += 1

# Generate (avoid spaces in note!)
s, b = req("POST", "/activation-codes/generate", {
    "planId": "plan_pro_month",
    "duration": 30,
    "quantity": 3,
    "note": "stability-test",
    "validDays": 90
}, AUTH)
gen_ok = s == 200
results["pass" if test("生成激活码", gen_ok) else "fail"] += 1

# Get a redeem code for testing
if gen_ok:
    test_code = b.get("data", {}).get("codes", [{}])[0].get("code")

# CSV export (returns raw CSV, not JSON)
import http.client
url = f"{BASE}/activation-codes/export/csv"
r = urllib.request.Request(url, headers=AUTH, method="GET")
try:
    with urllib.request.urlopen(r, timeout=10) as resp:
        csv_ok = resp.status == 200
except Exception:
    csv_ok = False
results["pass" if test("导出CSV", csv_ok) else "fail"] += 1

# Revoke
codes_list = b.get("data", {}).get("codes", [])
if len(codes_list) > 1:
    revoke_id = codes_list[1]["id"]
    s, b = req("PATCH", f"/activation-codes/{revoke_id}/revoke", headers=AUTH)
    results["pass" if test("撤销激活码", s == 200) else "fail"] += 1

# ===== 10. App API (App Key) =====
print("\n--- 10. App接口 ---")

# Verify membership
s, b = req("GET", "/verify/membership?appUserId=demo_user_001", headers={"X-App-Key": APP_KEY})
results["pass" if test("App会员校验", s == 200 and b.get("code") == 0) else "fail"] += 1

# Verify nonexistent user
s, b = req("GET", "/verify/membership?appUserId=no_such_user_000", headers={"X-App-Key": APP_KEY})
results["pass" if test("不存在的用户校验", s == 200 and not b.get("data", {}).get("isMember", True)) else "fail"] += 1

# Activate code
if gen_ok and test_code:
    s, b = req("POST", "/activate", {
        "appUserId": "stability-redeem-001",
        "code": test_code
    }, headers={"X-App-Key": APP_KEY})
    results["pass" if test("App激活码兑换", s == 200 and b.get("data", {}).get("success") == True) else "fail"] += 1

# ===== 11. Admins =====
print("\n--- 11. 管理员管理 ---")
s, b = req("GET", "/admins", headers=AUTH)
results["pass" if test("管理员列表", s == 200) else "fail"] += 1

# Change password (same password)
s, b = req("POST", "/admins/change-password", {
    "oldPassword": "admin123456",
    "newPassword": "admin123456"
}, AUTH)
results["pass" if test("修改密码(同密码)", s == 200) else "fail"] += 1

# ===== 12. Concurrent requests =====
print("\n--- 12. 并发测试 ---")
import threading
import queue

def concurrent_req(path, idx, results_q):
    try:
        s, _ = req("GET", path, headers=AUTH)
        results_q.put(("ok" if s == 200 else f"HTTP{s}", idx))
    except Exception as e:
        results_q.put(("err", idx))

# 10 concurrent dashboard requests
q = queue.Queue()
threads = []
for i in range(10):
    t = threading.Thread(target=concurrent_req, args=("/dashboard", i, q))
    threads.append(t)
    t.start()
for t in threads:
    t.join()

concurrent_results = []
while not q.empty():
    concurrent_results.append(q.get())

successes = sum(1 for r in concurrent_results if r[0] == "ok")
results["pass" if test("10并发请求", successes == 10, note=f"{successes}/10成功") else "fail"] += 1

# ===== 13. Rate limiting =====
print("\n--- 13. 速率限制 ---")
# Try hitting auth too many times rapidly
rate_limited = False
for i in range(15):
    s, _ = req("POST", "/auth/login", {"email": "admin@expensetracker.app", "password": "badpass"})
    if s == 429:
        rate_limited = True
        break
    time.sleep(0.03)

results["pass" if test("错误密码限流", rate_limited, note="多次错误后应触发429") else "fail"] += 1

# ===== 14. Health check at end =====
print("\n--- 14. 最终健康检查 ---")
time.sleep(1)
s, b = req("GET", "/../health")
results["pass" if test("服务仍在线", s == 200) else "fail"] += 1

# ===== Summary =====
print("\n" + "=" * 60)
print(f"测试完成: {results['pass']} 通过 / {results['pass'] + results['fail']} 总计")
if results["fail"] > 0:
    print(f"⚠️  {results['fail']} 项失败！")
else:
    print("🎉 全部通过！")
print("=" * 60)

sys.exit(0 if results["fail"] == 0 else 1)
