// 公共配置和工具函数
// 使用页面所在域名，自动适配本地（localhost）/ 内网 / 公网部署
const _host = window.location.hostname || "127.0.0.1";
const AppConfig = {
    // 基础地址配置
    sPayMallUrl: `http://${_host}:8070`,
    groupBuyMarketUrl: `http://${_host}:8091`,
    /** 拼团智能客服 Agent 静态页（ai-agent-group-buy-cs），默认同主机 8092；若 Nginx 反代为 https://域名/agent/ 请改为对应根路径 */
    customerServiceBaseUrl: `http://${_host}:8092`,
    goodsId: "9890001"
};

// 工具函数
const AppUtils = {
    // 获取Cookie值
    getCookie: function(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    },
    
    // 获取当前登录用户ID
    getCurrentUserId: function() {
        const userId = this.getCookie("loginToken");
        if (!userId) {
            window.location.href = "login.html"; // 跳转到登录页
            return null;
        }
        return userId;
    },
    
    // 混淆用户ID显示
    obfuscateUserId: function(userId) {
        if (userId.length <= 4) {
            // 如果 userId 的长度小于或等于 4，则无需替换任何字符
            return userId;
        } else {
            // 获取前两位和后两位
            const start = userId.slice(0, 2);
            const end = userId.slice(-2);
            // 计算中间部分应该被替换成多少个 *
            const middle = '*'.repeat(userId.length - 4);
            // 返回成功替换后的字符串
            return `${start}${middle}${end}`;
        }
    },
    
    // 从URL参数获取用户ID（可选功能）
    getUserIdFromUrl: function() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('userId') || this.getCurrentUserId();
    },

    /**
     * 打开拼团智能客服页（新标签），携带 userId、goodsId、source、channel。
     * 未登录时跳转登录页。
     * @param {string} [goodsId] 当前商品 ID，默认 AppConfig.goodsId 或 window.__currentGoodsId
     */
    openCustomerService: function(goodsId) {
        const userId = this.getCookie("loginToken");
        if (!userId) {
            window.location.href = "login.html";
            return;
        }
        const gid =
            goodsId ||
            (typeof window !== "undefined" && window.__currentGoodsId) ||
            AppConfig.goodsId;
        const base = (AppConfig.customerServiceBaseUrl || "").replace(/\/$/, "");
        const q = new URLSearchParams({
            userId: userId,
            goodsId: gid,
            source: "s01",
            channel: "c01"
        });
        window.open(base + "/group-buy-chat.html?" + q.toString(), "_blank");
    }
};

// 导出配置和工具函数到全局
window.AppConfig = AppConfig;
window.AppUtils = AppUtils;
window.getCookie = AppUtils.getCookie;
window.obfuscateUserId = AppUtils.obfuscateUserId;