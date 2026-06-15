-- ========================================
-- AI面试平台 - 完整四岗位题库
-- 包含：Java后端、Web前端、Python算法、游戏客户端
-- 每岗位：25道题，覆盖4类题型
-- 题型：TECH_KNOWLEDGE、SCENARIO、PROJECT_DEEP、BEHAVIOR
-- ========================================

-- ========================================
-- 1. JAVA_BACKEND 岗位题库（25题）
-- ========================================
INSERT INTO t_question (position_code, primary_kb_module_id, title, answer_reference, difficulty, question_type, topic, source) VALUES
-- TECH_KNOWLEDGE (10题)
('JAVA_BACKEND', 3, '请解释 Java 虚拟机（JVM）内存模型的组成及各区域的作用？', '堆、栈、方法区、程序计数器、本地方法栈；JDK8 元空间替代永久代', 2, 'TECH_KNOWLEDGE', 'JVM', 'MANUAL'),
('JAVA_BACKEND', 5, '请说明 HashMap 的底层实现原理，以及 JDK 8 的优化？', '数组+链表+红黑树；负载因子0.75；树化阈值8', 2, 'TECH_KNOWLEDGE', '集合框架', 'MANUAL'),
('JAVA_BACKEND', NULL, 'Java 中 String、StringBuffer、StringBuilder 的区别？', 'String 不可变；StringBuffer 线程安全但性能低；StringBuilder 线程不安全但性能高', 1, 'TECH_KNOWLEDGE', '基础', 'MANUAL'),
('JAVA_BACKEND', NULL, '请解释 Spring Bean 的生命周期？', '实例化 → 属性注入 → 初始化前 → 初始化 → 初始化后 → 使用 → 销毁', 2, 'TECH_KNOWLEDGE', 'Spring', 'MANUAL'),
('JAVA_BACKEND', NULL, '什么是 Spring AOP？应用场景有哪些？', '面向切面编程；日志、事务、权限、性能监控', 2, 'TECH_KNOWLEDGE', 'Spring', 'MANUAL'),
('JAVA_BACKEND', NULL, 'MySQL 索引的数据结构？为什么用 B+树？', 'B+树；数据只在叶子节点；范围查询快；页分裂优化', 2, 'TECH_KNOWLEDGE', 'MySQL', 'MANUAL'),
('JAVA_BACKEND', NULL, 'Redis 数据类型有哪些？各适用于什么场景？', 'String/List/Hash/Set/ZSet/BitMap/Geo/HyperLogLog/Stream', 2, 'TECH_KNOWLEDGE', 'Redis', 'MANUAL'),
('JAVA_BACKEND', NULL, '什么是 Java 内存模型（JMM）？volatile 的作用？', '主内存与工作内存；可见性、禁止指令重排、不保证原子性', 3, 'TECH_KNOWLEDGE', '并发', 'MANUAL'),
('JAVA_BACKEND', NULL, 'synchronized 和 ReentrantLock 的区别？', '关键字vs类；可重入；可中断；公平锁；Condition', 2, 'TECH_KNOWLEDGE', '并发', 'MANUAL'),
('JAVA_BACKEND', NULL, '请解释 JVM 垃圾回收算法？CMS 和 G1 的区别？', '标记清除、复制、标记整理；CMS 低延迟 vs G1 可预测停顿', 3, 'TECH_KNOWLEDGE', 'JVM', 'MANUAL'),
-- SCENARIO (5题)
('JAVA_BACKEND', NULL, '如何设计一个高并发的秒杀系统？请从架构层面阐述。', '限流、缓存、异步、库存扣减、消息队列、分布式锁', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('JAVA_BACKEND', NULL, '如何保证接口的幂等性？请列举常见方案。', '唯一ID、token机制、数据库唯一约束、乐观锁、redis setnx', 2, 'SCENARIO', '分布式', 'MANUAL'),
('JAVA_BACKEND', NULL, '如何防止重复提交？前端和后端分别怎么做？', '前端：防抖/状态禁用；后端：token/幂等键/乐观锁', 2, 'SCENARIO', '系统设计', 'MANUAL'),
('JAVA_BACKEND', NULL, '系统出现 OOM 如何排查？', 'heap dump、jmap/jhat、jstat、jvisualvm、MAT分析', 3, 'SCENARIO', '问题排查', 'MANUAL'),
('JAVA_BACKEND', NULL, '如何设计分布式系统的限流方案？', '滑动窗口、令牌桶、漏桶算法；Redis+Lua；Sentinel', 3, 'SCENARIO', '系统设计', 'MANUAL'),
-- PROJECT_DEEP (5题)
('JAVA_BACKEND', NULL, '请结合你简历中的项目，深入讲解你在项目中遇到的最大技术挑战及解决方案。', 'STAR 法则，结合具体技术细节', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('JAVA_BACKEND', NULL, '请描述一个你负责的复杂模块的设计与实现过程。', '需求分析、技术选型、架构设计、编码实现、测试上线', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('JAVA_BACKEND', NULL, '你在项目中是如何做性能优化的？请举具体例子说明。', '定位瓶颈（慢SQL/日志/锁）、优化方案、效果验证', 3, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('JAVA_BACKEND', NULL, '你有参与过线上问题排查吗？请描述一次印象深刻的排查经历。', '问题现象、排查思路、定位过程、解决方案、复盘总结', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('JAVA_BACKEND', NULL, '请讲讲你做过的最有成就感的项目，你在其中扮演了什么角色？', '项目背景、个人贡献、遇到困难、解决过程、成果与收获', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
-- BEHAVIOR (5题)
('JAVA_BACKEND', NULL, '请实现「两数之和」算法题', '哈希表 O(n) 解法', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('JAVA_BACKEND', NULL, '请实现「无重复字符的最长子串」', '滑动窗口 O(n)', 2, 'BEHAVIOR', '算法', 'LC_HOT100'),
('JAVA_BACKEND', NULL, '请实现「二叉树的层序遍历」', 'BFS 使用队列', 2, 'BEHAVIOR', '算法', 'LC_HOT100'),
('JAVA_BACKEND', NULL, '请实现「LRU缓存机制」', '哈希表 + 双向链表', 3, 'BEHAVIOR', '算法', 'LC_HOT100'),
('JAVA_BACKEND', NULL, '请实现「反转链表」', '迭代或递归', 1, 'BEHAVIOR', '算法', 'LC_HOT100');

-- ========================================
-- 2. WEB_FRONTEND 岗位题库（25题）
-- ========================================
INSERT INTO t_question (position_code, primary_kb_module_id, title, answer_reference, difficulty, question_type, topic, source) VALUES
-- TECH_KNOWLEDGE (10题)
('WEB_FRONTEND', NULL, '请解释 JavaScript 的事件循环机制？', '调用栈、宏任务/微任务队列、事件循环过程', 2, 'TECH_KNOWLEDGE', 'JS基础', 'MANUAL'),
('WEB_FRONTEND', NULL, 'ES6 有哪些新特性？请列举并简要说明。', 'let/const、箭头函数、解构、Promise、async/await、Map/Set、Class', 2, 'TECH_KNOWLEDGE', 'ES6', 'MANUAL'),
('WEB_FRONTEND', NULL, 'Vue 2 和 Vue 3 的区别？Composition API 的优势？', '响应式原理、虚拟DOM、Composition API、性能优化', 2, 'TECH_KNOWLEDGE', 'Vue', 'MANUAL'),
('WEB_FRONTEND', NULL, '请解释 Vue 的双向绑定原理？', 'Vue2：Object.defineProperty；Vue3：Proxy + Reflect', 2, 'TECH_KNOWLEDGE', 'Vue', 'MANUAL'),
('WEB_FRONTEND', NULL, '什么是 Virtual DOM？Diff 算法的原理？', '虚拟DOM对比；同层比较、key作用、双端对比', 3, 'TECH_KNOWLEDGE', 'Vue/React', 'MANUAL'),
('WEB_FRONTEND', NULL, 'CSS 盒模型？标准盒与怪异盒的区别？', 'content-box vs border-box', 1, 'TECH_KNOWLEDGE', 'CSS', 'MANUAL'),
('WEB_FRONTEND', NULL, '请列举 CSS 居中的方案？', 'flex、grid、margin auto、absolute+transform、table-cell', 2, 'TECH_KNOWLEDGE', 'CSS', 'MANUAL'),
('WEB_FRONTEND', NULL, '浏览器渲染流程？如何优化首屏加载？', '解析DOM/CSS → 渲染树 → 布局 → 绘制；资源优化、预加载、CDN', 2, 'TECH_KNOWLEDGE', '性能', 'MANUAL'),
('WEB_FRONTEND', NULL, 'HTTP 和 HTTPS 的区别？HTTP/2 的特性？', '加密、CA证书、端口；二进制分帧、多路复用、头部压缩、服务器推送', 2, 'TECH_KNOWLEDGE', '网络', 'MANUAL'),
('WEB_FRONTEND', NULL, '请解释 Webpack 的构建流程？', '入口、Loader、Plugin、Module、Chunk、Output', 2, 'TECH_KNOWLEDGE', '工程化', 'MANUAL'),
-- SCENARIO (5题)
('WEB_FRONTEND', NULL, '如何设计一个复杂的中后台系统？', '布局设计、权限管理、组件封装、状态管理、工程化', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('WEB_FRONTEND', NULL, '前端性能优化有哪些策略？', '资源加载、渲染优化、缓存策略、代码优化、监控体系', 2, 'SCENARIO', '性能优化', 'MANUAL'),
('WEB_FRONTEND', NULL, '如何做前端监控与埋点？', '错误监控、性能监控、用户行为埋点；sentry/自研', 2, 'SCENARIO', '监控', 'MANUAL'),
('WEB_FRONTEND', NULL, '如何解决跨域问题？', 'CORS、JSONP、代理、Nginx、postMessage', 2, 'SCENARIO', '网络', 'MANUAL'),
('WEB_FRONTEND', NULL, '请讲讲你是如何做组件设计与封装的？', '单一职责、可复用性、可扩展性、Props设计、插槽、文档', 3, 'SCENARIO', '系统设计', 'MANUAL'),
-- PROJECT_DEEP (5题)
('WEB_FRONTEND', NULL, '请结合你简历中的项目，深入讲解你在项目中遇到的最大技术挑战及解决方案。', 'STAR 法则，结合具体技术细节', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('WEB_FRONTEND', NULL, '请描述一个你负责的复杂组件的设计与实现过程。', '需求分析、技术方案、编码实现、测试、优化', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('WEB_FRONTEND', NULL, '你在项目中是如何做性能优化的？请举具体例子说明。', '定位瓶颈、优化方案、效果验证、数据对比', 3, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('WEB_FRONTEND', NULL, '你有参与过线上问题排查吗？请描述一次印象深刻的排查经历。', '问题现象、排查思路、定位过程、解决方案、复盘', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('WEB_FRONTEND', NULL, '请讲讲你做过的最有成就感的项目，你在其中扮演了什么角色？', '项目背景、个人贡献、遇到困难、解决过程、成果与收获', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
-- BEHAVIOR (5题) — 全部 LeetCode Hot 100
('WEB_FRONTEND', NULL, '请实现「两数之和」', '哈希表 O(n) 解法', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('WEB_FRONTEND', NULL, '请实现「无重复字符的最长子串」', '滑动窗口 O(n)', 2, 'BEHAVIOR', '算法', 'LC_HOT100'),
('WEB_FRONTEND', NULL, '请实现「有效的括号」', '栈匹配', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('WEB_FRONTEND', NULL, '请实现「最大子数组和」', 'Kadane 算法 O(n)', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('WEB_FRONTEND', NULL, '请实现「爬楼梯」', '动态规划 / 斐波那契', 1, 'BEHAVIOR', '算法', 'LC_HOT100');

-- ========================================
-- 3. PYTHON_ALGO 岗位题库（25题）
-- ========================================
INSERT INTO t_question (position_code, primary_kb_module_id, title, answer_reference, difficulty, question_type, topic, source) VALUES
-- TECH_KNOWLEDGE (10题)
('PYTHON_ALGO', NULL, 'Python 的 GIL 是什么？对多线程有什么影响？', '全局解释器锁；同一时刻只有一个线程执行CPU密集代码', 2, 'TECH_KNOWLEDGE', 'Python', 'MANUAL'),
('PYTHON_ALGO', NULL, '请解释 *args 和 **kwargs 的用法？', '可变位置参数、可变关键字参数', 1, 'TECH_KNOWLEDGE', 'Python', 'MANUAL'),
('PYTHON_ALGO', NULL, '什么是装饰器？请举例子说明。', '闭包实现、@语法糖、函数增强', 2, 'TECH_KNOWLEDGE', 'Python', 'MANUAL'),
('PYTHON_ALGO', NULL, '迭代器、生成器、可迭代对象的区别？', '__iter__/__next__；yield；iter()', 2, 'TECH_KNOWLEDGE', 'Python', 'MANUAL'),
('PYTHON_ALGO', NULL, '请解释 Python 的深浅拷贝？', 'copy.copy vs copy.deepcopy；可变对象嵌套', 2, 'TECH_KNOWLEDGE', 'Python', 'MANUAL'),
('PYTHON_ALGO', NULL, '什么是动态规划？能举个例子吗？', '重叠子问题、最优子结构；背包问题、爬楼梯', 2, 'TECH_KNOWLEDGE', '算法', 'MANUAL'),
('PYTHON_ALGO', NULL, '请解释二分查找及其时间复杂度？', '有序数组、O(log n)、边界处理', 1, 'TECH_KNOWLEDGE', '算法', 'MANUAL'),
('PYTHON_ALGO', NULL, '什么是快速排序？请说明其原理和时间复杂度？', '分治、基准选择、O(n log n)平均、O(n²)最坏', 2, 'TECH_KNOWLEDGE', '算法', 'MANUAL'),
('PYTHON_ALGO', NULL, '请解释二叉树的前/中/后序遍历？', '根左右、左根右、左右根', 1, 'TECH_KNOWLEDGE', '数据结构', 'MANUAL'),
('PYTHON_ALGO', NULL, '什么是哈希冲突？有哪些解决方法？', '链地址法、开放寻址法、再哈希法', 2, 'TECH_KNOWLEDGE', '数据结构', 'MANUAL'),
-- SCENARIO (5题)
('PYTHON_ALGO', NULL, '如何设计一个推荐系统？请从算法和工程层面阐述。', '协同过滤、内容推荐、召回+排序、特征工程、冷启动', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('PYTHON_ALGO', NULL, '如果模型在训练集上效果很好，但测试集上效果很差，你怎么分析？', '过拟合、数据分布差异、特征泄露、模型选择', 2, 'SCENARIO', 'ML', 'MANUAL'),
('PYTHON_ALGO', NULL, '如何处理数据集中的缺失值？', '删除、均值/中位数/众数填充、模型预测、KNN填充', 2, 'SCENARIO', '数据处理', 'MANUAL'),
('PYTHON_ALGO', NULL, '请说明如何优化一个慢查询或慢算法？', '算法复杂度分析、空间换时间、并行计算、剪枝', 3, 'SCENARIO', '性能优化', 'MANUAL'),
('PYTHON_ALGO', NULL, '如何设计一个爬虫系统？', '请求池、去重、并发、反爬应对、数据存储', 2, 'SCENARIO', '系统设计', 'MANUAL'),
-- PROJECT_DEEP (5题)
('PYTHON_ALGO', NULL, '请结合你简历中的项目，深入讲解你在项目中遇到的最大技术挑战及解决方案。', 'STAR 法则，结合具体技术细节', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('PYTHON_ALGO', NULL, '请描述一个你做过的算法或模型优化项目，说明优化前后的效果。', '问题背景、基线方案、优化思路、实施过程、效果评估', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('PYTHON_ALGO', NULL, '你在项目中是如何做特征工程的？请举具体例子。', '特征选择、特征构造、特征变换、特征重要性', 3, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('PYTHON_ALGO', NULL, '你有遇到过模型上线效果不如离线训练的情况吗？如何解决？', '线上线下数据差异、特征不一致、概念漂移', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('PYTHON_ALGO', NULL, '请讲讲你做过的最有成就感的项目，你在其中扮演了什么角色？', '项目背景、个人贡献、遇到困难、解决过程、成果与收获', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
-- BEHAVIOR (5题)
('PYTHON_ALGO', NULL, '请实现「两数之和」', '哈希表 O(n)', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('PYTHON_ALGO', NULL, '请实现「最长回文子串」', '动态规划或中心扩散', 2, 'BEHAVIOR', '算法', 'LC_HOT100'),
('PYTHON_ALGO', NULL, '请实现「二叉树的最大深度」', '递归或 BFS', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('PYTHON_ALGO', NULL, '请实现「买卖股票的最佳时机」', '一次遍历记录最小值', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('PYTHON_ALGO', NULL, '请实现「最长递增子序列」', 'DP O(n²) 或二分 O(n log n)', 2, 'BEHAVIOR', '算法', 'LC_HOT100');

-- ========================================
-- 4. GAME_CLIENT 岗位题库（25题）
-- ========================================
INSERT INTO t_question (position_code, primary_kb_module_id, title, answer_reference, difficulty, question_type, topic, source) VALUES
-- TECH_KNOWLEDGE (10题)
('GAME_CLIENT', NULL, '请解释 Unity 的 GameObject 和 Component 关系？', '实体-组件模式，GameObject 是容器，Component 是功能模块', 2, 'TECH_KNOWLEDGE', 'Unity', 'MANUAL'),
('GAME_CLIENT', NULL, 'Unity 的生命周期函数有哪些？执行顺序是怎样的？', 'Awake、Start、Update、LateUpdate、FixedUpdate、OnDestroy', 2, 'TECH_KNOWLEDGE', 'Unity', 'MANUAL'),
('GAME_CLIENT', NULL, '请解释什么是 Draw Call？如何优化 Draw Call？', 'GPU 绘制命令；合批、图集、静态批处理、SRP Batcher', 3, 'TECH_KNOWLEDGE', '渲染', 'MANUAL'),
('GAME_CLIENT', NULL, '什么是对象池？为什么需要对象池？', '预分配对象复用；减少 GC、避免频繁创建销毁', 2, 'TECH_KNOWLEDGE', '性能', 'MANUAL'),
('GAME_CLIENT', NULL, '请解释游戏中的碰撞检测原理？', 'AABB、OBB、Sphere、Raycast；宽相位+窄相位', 2, 'TECH_KNOWLEDGE', '物理', 'MANUAL'),
('GAME_CLIENT', NULL, 'Unity 的协程是什么？与线程有什么区别？', '迭代器实现、主线程分帧执行、非多线程', 2, 'TECH_KNOWLEDGE', 'Unity', 'MANUAL'),
('GAME_CLIENT', NULL, '请解释 Shader 的基本结构？Vertex Shader 和 Fragment Shader 的作用？', '顶点变换、片元着色；顶点位置、像素颜色', 3, 'TECH_KNOWLEDGE', '渲染', 'MANUAL'),
('GAME_CLIENT', NULL, '游戏中的动画系统有哪些？', 'Legacy、Animator、Playable、Animation Rigging', 2, 'TECH_KNOWLEDGE', '动画', 'MANUAL'),
('GAME_CLIENT', NULL, '请解释什么是 ECS？与传统 OOP 的区别？', '实体-组件-系统；数据驱动、缓存友好、高性能', 3, 'TECH_KNOWLEDGE', '架构', 'MANUAL'),
('GAME_CLIENT', NULL, '什么是帧同步和状态同步？各适用于什么场景？', '输入同步vs状态同步；MOBA vs RPG/FPS', 3, 'TECH_KNOWLEDGE', '网络', 'MANUAL'),
-- SCENARIO (5题)
('GAME_CLIENT', NULL, '如何设计一个灵活的技能系统？', '技能配置、Buff系统、事件机制、状态机', 3, 'SCENARIO', '系统设计', 'MANUAL'),
('GAME_CLIENT', NULL, '游戏卡顿如何排查与优化？', 'Profiler、CPU/GPU分析、定位瓶颈、分步优化', 2, 'SCENARIO', '性能优化', 'MANUAL'),
('GAME_CLIENT', NULL, '如何做游戏内存优化？', '资源压缩、对象池、资源卸载、纹理格式优化', 3, 'SCENARIO', '内存', 'MANUAL'),
('GAME_CLIENT', NULL, '请讲讲你是如何设计游戏中的 UI 架构的？', 'UI管理、分层设计、事件系统、MVVM/MVC', 2, 'SCENARIO', '系统设计', 'MANUAL'),
('GAME_CLIENT', NULL, '如何做游戏热更新？', 'Lua/ILRuntime/CLR、AssetBundle、热更方案选择', 3, 'SCENARIO', '热更新', 'MANUAL'),
-- PROJECT_DEEP (5题)
('GAME_CLIENT', NULL, '请结合你简历中的项目，深入讲解你在项目中遇到的最大技术挑战及解决方案。', 'STAR 法则，结合具体技术细节', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('GAME_CLIENT', NULL, '请描述一个你负责的复杂游戏系统的设计与实现过程。', '需求分析、技术方案、架构设计、编码实现、测试', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('GAME_CLIENT', NULL, '你在游戏项目中是如何做性能优化的？请举具体例子。', '性能分析、定位瓶颈、优化方案、效果验证', 3, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('GAME_CLIENT', NULL, '你有参与过游戏线上问题排查吗？请描述一次印象深刻的经历。', '问题现象、排查思路、定位过程、解决方案、复盘', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
('GAME_CLIENT', NULL, '请讲讲你做过的最有成就感的游戏项目，你在其中扮演了什么角色？', '项目背景、个人贡献、遇到困难、解决过程、成果与收获', 2, 'PROJECT_DEEP', '项目经验', 'MANUAL'),
-- BEHAVIOR (5题) — 全部 LeetCode Hot 100
('GAME_CLIENT', NULL, '请实现「反转链表」', '迭代或递归', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('GAME_CLIENT', NULL, '请实现「环形链表」', '快慢指针 / Floyd', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('GAME_CLIENT', NULL, '请实现「二叉树的最大深度」', '递归 DFS 或 BFS', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('GAME_CLIENT', NULL, '请实现「打家劫舍」', '动态规划', 1, 'BEHAVIOR', '算法', 'LC_HOT100'),
('GAME_CLIENT', NULL, '请实现「最长递增子序列」', 'DP 或二分搜索', 2, 'BEHAVIOR', '算法', 'LC_HOT100');
