/**
 * 书香拼团：商品展示 manifest（静态页用；营销试算仍以 goodsId 走接口）
 */
(function () {
    var DEF = "9890001";

    var BOOK_PAGES = {
        "9890001": {
            docTitle: "拼团商城 - 手写MyBatis",
            title: "手写MyBatis：渐进式源码实践（全彩）",
            meta: [
                { label: "作者", value: "LZJ 著" },
                { label: "出版社", value: "电子工堂出版社" },
                { label: "ISBN", value: "978-7-121-45678-9" },
                { label: "装帧", value: "全彩印刷" }
            ],
            slides: [
                { src: "./images/book-cover.png", alt: "《手写MyBatis》封面" },
                { src: "./images/book-spread.png", alt: "封面与封底" },
                { src: "./images/book-inner.png", alt: "内页预览" }
            ],
            highlights: [
                { title: "由浅入深，循序渐进", desc: "从零开始，一步步实现 MyBatis 核心功能" },
                { title: "源码级剖析", desc: "覆盖核心模块，讲透设计思想与实现细节" },
                { title: "全彩印刷，图文并茂", desc: "架构图 + 流程图 + 代码图，学习更轻松" },
                { title: "实战导向", desc: "丰富案例贯穿全书，助力快速掌握与落地" }
            ]
        },
        "9890002": {
            docTitle: "拼团商城 - Spring Boot 实战",
            title: "Spring Boot 实战（第2版）",
            meta: [
                { label: "作者", value: "Craig Walls 著" },
                { label: "出版社", value: "技术图书出版社" },
                { label: "ISBN", value: "978-7-121-50002-1" },
                { label: "装帧", value: "平装" }
            ],
            slides: [
                { src: "./images/book-cover.png", alt: "《Spring Boot 实战》封面" },
                { src: "./images/book-spread.png", alt: "书影展开" },
                { src: "./images/book-inner.png", alt: "章节内页" }
            ],
            highlights: [
                { title: "自动配置与起步依赖", desc: "掌握 Spring Boot 核心装配与约定优于配置" },
                { title: "Web 与安全", desc: "REST、模板引擎、Actuator 与健康检查" },
                { title: "数据与消息", desc: "JPA、JDBC、非关系型数据与集成测试" },
                { title: "部署与运维", desc: "可执行 JAR、云部署与生产级调优要点" }
            ]
        },
        "9890003": {
            docTitle: "拼团商城 - Redis 设计与实现",
            title: "Redis 设计与实现",
            meta: [
                { label: "作者", value: "黄健宏 著" },
                { label: "出版社", value: "技术图书出版社" },
                { label: "ISBN", value: "978-7-121-50003-8" },
                { label: "装帧", value: "平装" }
            ],
            slides: [
                { src: "./images/book-inner.png", alt: "《Redis 设计与实现》内页示意" },
                { src: "./images/book-cover.png", alt: "封面" },
                { src: "./images/book-spread.png", alt: "书脊与封底" }
            ],
            highlights: [
                { title: "数据结构源码级讲解", desc: "字符串、列表、哈希、集合、有序集合等实现" },
                { title: "持久化与事件循环", desc: "RDB、AOF 与文件事件、时间事件" },
                { title: "多机特性", desc: "复制、Sentinel 与集群路由" },
                { title: "配套练习", desc: "便于对照 Redis 版本动手验证" }
            ]
        },
        "9890004": {
            docTitle: "拼团商城 - 深入理解 Java 虚拟机",
            title: "深入理解 Java 虚拟机（第3版）",
            meta: [
                { label: "作者", value: "周志明 著" },
                { label: "出版社", value: "技术图书出版社" },
                { label: "ISBN", value: "978-7-121-50004-5" },
                { label: "装帧", value: "精装" }
            ],
            slides: [
                { src: "./images/book-spread.png", alt: "《深入理解 JVM》书影" },
                { src: "./images/book-cover.png", alt: "封面" },
                { src: "./images/book-inner.png", alt: "目录与插图" }
            ],
            highlights: [
                { title: "内存与垃圾回收", desc: "运行时数据区、GC 算法与调优思路" },
                { title: "类文件与类加载", desc: "字节码、虚拟机栈与类加载器协作" },
                { title: "编译与执行", desc: "前端编译、后端编译与即时编译器" },
                { title: "工程案例", desc: "结合线上问题排查与性能分析" }
            ]
        },
        "9890005": {
            docTitle: "拼团商城 - RabbitMQ 实战指南",
            title: "RabbitMQ 实战指南",
            meta: [
                { label: "作者", value: "项目组 编著" },
                { label: "出版社", value: "技术图书出版社" },
                { label: "ISBN", value: "978-7-121-50005-2" },
                { label: "装帧", value: "平装" }
            ],
            slides: [
                { src: "./images/book-cover.png", alt: "《RabbitMQ 实战指南》封面" },
                { src: "./images/book-inner.png", alt: "架构示意图页" },
                { src: "./images/book-spread.png", alt: "书影" }
            ],
            highlights: [
                { title: "AMQP 与模型", desc: "交换机、队列、绑定与路由键" },
                { title: "可靠投递", desc: "确认、重试、死信与幂等消费" },
                { title: "运维与监控", desc: "集群、镜像队列与指标采集" },
                { title: "与 Spring 集成", desc: "典型异步解耦与削峰填谷场景" }
            ]
        }
    };

    function pageOrDefault(goodsId) {
        return BOOK_PAGES[goodsId] || BOOK_PAGES[DEF];
    }

    function applyBookPageToDom(goodsId) {
        var page = pageOrDefault(goodsId);
        document.title = page.docTitle;

        var h1 = document.querySelector(".product-title");
        if (h1) h1.textContent = page.title;

        var metaRoot = document.querySelector(".book-meta");
        if (metaRoot && page.meta && page.meta.length) {
            metaRoot.innerHTML = page.meta
                .map(function (m) {
                    return (
                        '<div class="meta-item"><span class="label">' +
                        m.label +
                        '</span><span class="value">' +
                        m.value +
                        "</span></div>"
                    );
                })
                .join("");
        }

        var grid = document.querySelector(".highlight-grid");
        if (grid && page.highlights && page.highlights.length) {
            grid.innerHTML = page.highlights
                .map(function (h) {
                    return (
                        '<div class="highlight-item"><div class="h-title">' +
                        h.title +
                        '</div><div class="h-desc">' +
                        h.desc +
                        "</div></div>"
                    );
                })
                .join("");
        }

        var wrapper = document.querySelector(".swiper-wrapper");
        if (wrapper && page.slides && page.slides.length) {
            wrapper.innerHTML = page.slides
                .map(function (s) {
                    return (
                        '<div class="swiper-slide"><img src="' +
                        s.src +
                        '" alt="' +
                        (s.alt || "").replace(/"/g, "&quot;") +
                        '"></div>'
                    );
                })
                .join("");
        }
    }

    var CATALOG_BOOKS = [
        {
            goodsId: "9890001",
            title: "手写MyBatis：渐进式源码实践",
            blurb: "渐进式源码实践，全彩印刷",
            listPrice: 100,
            cover: "./images/book-cover.png"
        },
        {
            goodsId: "9890002",
            title: "Spring Boot 实战（第2版）",
            blurb: "自动配置、Web、数据与部署",
            listPrice: 89,
            cover: "./images/book-cover.png"
        },
        {
            goodsId: "9890003",
            title: "Redis 设计与实现",
            blurb: "数据结构、持久化与多机特性",
            listPrice: 79,
            cover: "./images/book-cover.png"
        },
        {
            goodsId: "9890004",
            title: "深入理解 Java 虚拟机（第3版）",
            blurb: "内存管理、类加载与即时编译",
            listPrice: 119,
            cover: "./images/book-cover.png"
        },
        {
            goodsId: "9890005",
            title: "RabbitMQ 实战指南",
            blurb: "AMQP、可靠投递与 Spring 集成",
            listPrice: 69,
            cover: "./images/book-cover.png"
        }
    ];

    window.BOOK_PAGES = BOOK_PAGES;
    window.CATALOG_BOOKS = CATALOG_BOOKS;
    window.applyBookPageToDom = applyBookPageToDom;

    if (document.body && document.querySelector(".product-info")) {
        var params = new URLSearchParams(window.location.search);
        var gid = params.get("goodsId") || (window.AppConfig && window.AppConfig.goodsId) || DEF;
        window.__currentGoodsId = gid;
        applyBookPageToDom(gid);
    }
})();
