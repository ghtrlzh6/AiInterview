-- ============================================================
-- 手撕题统一为 LeetCode Hot 100 补丁
-- 适用于已部署的数据库：软删除旧 MANUAL 手撕题，插入/更新 Hot100 题，重新关联题池
-- 执行前请先执行 patch-coding-judge.sql
-- ============================================================

USE ai_interview;

-- 1) 软删除所有旧版 MANUAL 手撕题（MyBatis-Plus 逻辑删除后不再被抽题）
UPDATE t_question SET is_deleted = 1
WHERE question_type = 'BEHAVIOR' AND source = 'MANUAL';

-- 2) PYTHON_ALGO：将「快速排序」改为 Hot100「最长递增子序列」（若仍存在）
UPDATE t_question SET
  title = '请实现「最长递增子序列」',
  answer_reference = 'DP O(n²) 或二分 O(n log n)',
  source = 'LC_HOT100',
  difficulty = 2,
  topic = '算法',
  is_deleted = 0
WHERE position_code = 'PYTHON_ALGO'
  AND question_type = 'BEHAVIOR'
  AND title LIKE '%快速排序%';

-- 3) 为 WEB_FRONTEND / GAME_CLIENT 插入 Hot100 手撕题（不存在则插入）
INSERT INTO t_question (position_code, title, answer_reference, difficulty, question_type, topic, source)
SELECT * FROM (
  SELECT 'WEB_FRONTEND' AS pc, '请实现「两数之和」' AS t, '哈希表 O(n) 解法' AS ar, 1 AS d, 'BEHAVIOR' AS qt, '算法' AS tp, 'LC_HOT100' AS src UNION ALL
  SELECT 'WEB_FRONTEND', '请实现「无重复字符的最长子串」', '滑动窗口 O(n)', 2, 'BEHAVIOR', '算法', 'LC_HOT100' UNION ALL
  SELECT 'WEB_FRONTEND', '请实现「有效的括号」', '栈匹配', 1, 'BEHAVIOR', '算法', 'LC_HOT100' UNION ALL
  SELECT 'WEB_FRONTEND', '请实现「最大子数组和」', 'Kadane 算法 O(n)', 1, 'BEHAVIOR', '算法', 'LC_HOT100' UNION ALL
  SELECT 'WEB_FRONTEND', '请实现「爬楼梯」', '动态规划 / 斐波那契', 1, 'BEHAVIOR', '算法', 'LC_HOT100' UNION ALL
  SELECT 'GAME_CLIENT', '请实现「反转链表」', '迭代或递归', 1, 'BEHAVIOR', '算法', 'LC_HOT100' UNION ALL
  SELECT 'GAME_CLIENT', '请实现「环形链表」', '快慢指针 / Floyd', 1, 'BEHAVIOR', '算法', 'LC_HOT100' UNION ALL
  SELECT 'GAME_CLIENT', '请实现「二叉树的最大深度」', '递归 DFS 或 BFS', 1, 'BEHAVIOR', '算法', 'LC_HOT100' UNION ALL
  SELECT 'GAME_CLIENT', '请实现「打家劫舍」', '动态规划', 1, 'BEHAVIOR', '算法', 'LC_HOT100' UNION ALL
  SELECT 'GAME_CLIENT', '请实现「最长递增子序列」', 'DP 或二分搜索', 2, 'BEHAVIOR', '算法', 'LC_HOT100'
) AS new_q
WHERE NOT EXISTS (
  SELECT 1 FROM t_question q
  WHERE q.position_code = new_q.pc AND q.title = new_q.t
    AND q.question_type = 'BEHAVIOR' AND q.is_deleted = 0
);

-- 4) 确保 PYTHON_ALGO「最长递增子序列」存在（快速排序已改则跳过）
INSERT INTO t_question (position_code, title, answer_reference, difficulty, question_type, topic, source)
SELECT 'PYTHON_ALGO', '请实现「最长递增子序列」', 'DP O(n²) 或二分 O(n log n)', 2, 'BEHAVIOR', '算法', 'LC_HOT100'
WHERE NOT EXISTS (
  SELECT 1 FROM t_question
  WHERE position_code = 'PYTHON_ALGO' AND question_type = 'BEHAVIOR'
    AND title LIKE '%最长递增子序列%' AND is_deleted = 0
);

-- 5) 关联所有 LC_HOT100 手撕题到 t_coding_challenge
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-001' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%两数之和%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-003' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%无重复字符的最长子串%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-005' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%最长回文子串%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-020' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%有效的括号%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-053' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%最大子数组和%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-070' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%爬楼梯%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-102' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%二叉树的层序遍历%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-104' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%二叉树的最大深度%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-121' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%买卖股票的最佳时机%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-141' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%环形链表%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-146' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%LRU缓存%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-198' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%打家劫舍%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-206' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%反转链表%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-300' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%最长递增子序列%' AND q.is_deleted = 0;
UPDATE t_question q JOIN t_coding_challenge c ON c.external_ref = 'Hot100-322' SET q.coding_challenge_id = c.id WHERE q.question_type = 'BEHAVIOR' AND q.source = 'LC_HOT100' AND q.title LIKE '%零钱兑换%' AND q.is_deleted = 0;
