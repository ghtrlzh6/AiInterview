-- ============================================================
-- 手撕代码评判配置补丁 v1
-- 为 t_coding_challenge 添加 judge_config（测试用例）和 starter_code（起始代码）
-- 为 t_session_coding_submit 添加运行结果字段
-- 兼容 MySQL 5.7 / 8.0（不使用 ADD COLUMN IF NOT EXISTS）
-- ============================================================

USE ai_interview;

SET @db = DATABASE();

-- t_coding_challenge.judge_config
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_coding_challenge' AND COLUMN_NAME = 'judge_config') = 0,
  'ALTER TABLE t_coding_challenge ADD COLUMN judge_config JSON DEFAULT NULL COMMENT ''评判配置: testCases/inputFormat/outputFormat/timeLimit''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_coding_challenge.starter_code
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_coding_challenge' AND COLUMN_NAME = 'starter_code') = 0,
  'ALTER TABLE t_coding_challenge ADD COLUMN starter_code JSON DEFAULT NULL COMMENT ''各语言起始代码模板 {java, python, cpp, javascript}''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_session_coding_submit.run_status
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_session_coding_submit' AND COLUMN_NAME = 'run_status') = 0,
  'ALTER TABLE t_session_coding_submit ADD COLUMN run_status VARCHAR(20) DEFAULT NULL COMMENT ''PASSED/FAILED/ERROR/TIMEOUT/PENDING''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_session_coding_submit.tests_passed
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_session_coding_submit' AND COLUMN_NAME = 'tests_passed') = 0,
  'ALTER TABLE t_session_coding_submit ADD COLUMN tests_passed INT DEFAULT 0 COMMENT ''通过测试用例数''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_session_coding_submit.tests_total
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_session_coding_submit' AND COLUMN_NAME = 'tests_total') = 0,
  'ALTER TABLE t_session_coding_submit ADD COLUMN tests_total INT DEFAULT 0 COMMENT ''总测试用例数''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_session_coding_submit.run_stdout
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_session_coding_submit' AND COLUMN_NAME = 'run_stdout') = 0,
  'ALTER TABLE t_session_coding_submit ADD COLUMN run_stdout MEDIUMTEXT DEFAULT NULL COMMENT ''执行标准输出''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_session_coding_submit.run_stderr
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_session_coding_submit' AND COLUMN_NAME = 'run_stderr') = 0,
  'ALTER TABLE t_session_coding_submit ADD COLUMN run_stderr TEXT DEFAULT NULL COMMENT ''执行标准错误''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- Hot100-001: 两数之和
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给定一个整数数组 `nums` 和一个整数目标值 `target`，请你在该数组中找出**和为目标值**的那两个整数，并返回它们的**数组下标**（下标从 0 开始）。\n\n每种输入只会对应一个答案，且同一元素不能重复使用。\n\n---\n\n**输入格式**\n```\n第一行：数组长度 n\n第二行：n 个整数，空格分隔\n第三行：目标值 target\n```\n\n**输出格式**\n```\n两个下标，空格分隔（较小的下标在前）\n```\n\n**示例**\n```\n输入:\n4\n2 7 11 15\n9\n输出:\n0 1\n```\n\n**提示：** 哈希表可将时间复杂度从 O(n²) 降至 O(n)',
  judge_config = JSON_OBJECT(
    'inputFormat', '第一行：n\n第二行：n 个整数\n第三行：target',
    'outputFormat', '两个下标，空格分隔',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '4\n2 7 11 15\n9',   'expected', '0 1', 'description', '示例 1'),
      JSON_OBJECT('input', '3\n3 2 4\n6',        'expected', '1 2', 'description', '示例 2'),
      JSON_OBJECT('input', '2\n3 3\n6',          'expected', '0 1', 'description', '示例 3')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static int[] twoSum(int[] nums, int target) {\n        // TODO: 使用哈希表实现 O(n) 解法\n        return new int[]{};\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] nums = new int[n];\n        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n        int target = sc.nextInt();\n        int[] res = twoSum(nums, target);\n        System.out.println(res[0] + " " + res[1]);\n    }\n}',
    'python',
    'import sys\ninput = sys.stdin.readline\n\ndef two_sum(nums, target):\n    # TODO: 使用哈希表实现 O(n) 解法\n    pass\n\nn = int(input())\nnums = list(map(int, input().split()))\ntarget = int(input())\nres = two_sum(nums, target)\nprint(res[0], res[1])',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\n\nvector<int> twoSum(vector<int>& nums, int target) {\n    // TODO: 使用哈希表实现 O(n) 解法\n    return {};\n}\n\nint main() {\n    int n; cin >> n;\n    vector<int> nums(n);\n    for (int& x : nums) cin >> x;\n    int target; cin >> target;\n    auto res = twoSum(nums, target);\n    cout << res[0] << " " << res[1] << endl;\n    return 0;\n}'
  )
WHERE external_ref = 'Hot100-001';

-- ============================================================
-- Hot100-003: 无重复字符的最长子串
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给定一个字符串 `s`，请你找出其中**不含有重复字符**的**最长子串**的长度。\n\n---\n\n**输入格式**\n```\n一行：字符串 s\n```\n\n**输出格式**\n```\n最长子串的长度（整数）\n```\n\n**示例**\n```\n输入: abcabcbb\n输出: 3  （子串 "abc"）\n\n输入: bbbbb\n输出: 1  （子串 "b"）\n```\n\n**提示：** 使用滑动窗口，时间复杂度 O(n)',
  judge_config = JSON_OBJECT(
    'inputFormat', '一行字符串 s',
    'outputFormat', '最长无重复子串长度',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', 'abcabcbb', 'expected', '3', 'description', '示例 1'),
      JSON_OBJECT('input', 'bbbbb',    'expected', '1', 'description', '示例 2'),
      JSON_OBJECT('input', 'pwwkew',   'expected', '3', 'description', '示例 3')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static int lengthOfLongestSubstring(String s) {\n        // TODO: 滑动窗口解法\n        return 0;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String s = sc.nextLine().trim();\n        System.out.println(lengthOfLongestSubstring(s));\n    }\n}',
    'python',
    'import sys\n\ndef length_of_longest_substring(s):\n    # TODO: 滑动窗口解法\n    pass\n\ns = sys.stdin.readline().strip()\nprint(length_of_longest_substring(s))',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\n\nint lengthOfLongestSubstring(string s) {\n    // TODO: 滑动窗口解法\n    return 0;\n}\n\nint main() {\n    string s; getline(cin, s);\n    cout << lengthOfLongestSubstring(s) << endl;\n    return 0;\n}'
  )
WHERE external_ref = 'Hot100-003';

-- ============================================================
-- Hot100-005: 最长回文子串
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给你一个字符串 `s`，找到 `s` 中最长的**回文子串**并返回它。\n\n---\n\n**输入格式**\n```\n一行：字符串 s（1 ≤ s.length ≤ 1000）\n```\n\n**输出格式**\n```\n最长回文子串（若有多个长度相同的，输出任意一个）\n```\n\n**示例**\n```\n输入: babad\n输出: bab  （或 "aba"）\n\n输入: cbbd\n输出: bb\n```\n\n**提示：** 中心扩展法 O(n²) 或动态规划 O(n²)',
  judge_config = JSON_OBJECT(
    'inputFormat', '一行字符串 s',
    'outputFormat', '最长回文子串',
    'timeLimit', 2000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', 'cbbd', 'expected', 'bb',  'description', '示例 1'),
      JSON_OBJECT('input', 'a',    'expected', 'a',   'description', '示例 2'),
      JSON_OBJECT('input', 'ac',   'expected', 'a',   'description', '示例 3（返回较短）')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static String longestPalindrome(String s) {\n        // TODO: 中心扩展法或动态规划\n        return "";\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String s = sc.nextLine().trim();\n        System.out.println(longestPalindrome(s));\n    }\n}',
    'python',
    'import sys\n\ndef longest_palindrome(s):\n    # TODO: 中心扩展法或动态规划\n    pass\n\ns = sys.stdin.readline().strip()\nprint(longest_palindrome(s))',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\n\nstring longestPalindrome(string s) {\n    // TODO\n    return "";\n}\n\nint main() {\n    string s; getline(cin, s);\n    cout << longestPalindrome(s) << endl;\n    return 0;\n}'
  )
WHERE external_ref = 'Hot100-005';

-- ============================================================
-- Hot100-020: 有效的括号
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给定一个只包括 `(`、`)`、`{`、`}`、`[`、`]` 的字符串 `s`，判断字符串是否**有效**。\n\n有效字符串须满足：\n1. 左括号必须用相同类型的右括号闭合\n2. 左括号必须以正确的顺序闭合\n3. 每个右括号都有一个对应的左括号\n\n---\n\n**输入格式**\n```\n一行括号字符串\n```\n\n**输出格式**\n```\ntrue 或 false\n```\n\n**示例**\n```\n输入: ()[]{}\n输出: true\n\n输入: (]\n输出: false\n```\n\n**提示：** 使用栈，时间复杂度 O(n)',
  judge_config = JSON_OBJECT(
    'inputFormat', '一行括号字符串',
    'outputFormat', 'true 或 false',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '()',     'expected', 'true',  'description', '示例 1'),
      JSON_OBJECT('input', '()[]{}\n', 'expected', 'true',  'description', '示例 2'),
      JSON_OBJECT('input', '(]',    'expected', 'false', 'description', '示例 3'),
      JSON_OBJECT('input', '([)]',  'expected', 'false', 'description', '示例 4'),
      JSON_OBJECT('input', '{[]}',  'expected', 'true',  'description', '示例 5')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static boolean isValid(String s) {\n        // TODO: 使用栈实现\n        return false;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String s = sc.nextLine().trim();\n        System.out.println(isValid(s));\n    }\n}',
    'python',
    'import sys\n\ndef is_valid(s):\n    # TODO: 使用栈实现\n    pass\n\ns = sys.stdin.readline().strip()\nprint(str(is_valid(s)).lower())',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\n\nbool isValid(string s) {\n    // TODO\n    return false;\n}\n\nint main() {\n    string s; getline(cin, s);\n    cout << (isValid(s) ? "true" : "false") << endl;\n    return 0;\n}'
  )
WHERE external_ref = 'Hot100-020';

-- ============================================================
-- Hot100-053: 最大子数组和
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给你一个整数数组 `nums`，请你找出一个具有最大和的连续子数组（子数组最少包含一个元素），返回其最大和。\n\n---\n\n**输入格式**\n```\n第一行：数组长度 n\n第二行：n 个整数，空格分隔\n```\n\n**输出格式**\n```\n最大子数组和（整数）\n```\n\n**示例**\n```\n输入:\n9\n-2 1 -3 4 -1 2 1 -5 4\n输出: 6  （子数组 [4,-1,2,1]）\n```\n\n**提示：** Kadane 算法，时间复杂度 O(n)',
  judge_config = JSON_OBJECT(
    'inputFormat', '第一行：n\n第二行：n 个整数',
    'outputFormat', '最大子数组和',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '9\n-2 1 -3 4 -1 2 1 -5 4', 'expected', '6',  'description', '示例 1'),
      JSON_OBJECT('input', '1\n1',                       'expected', '1',  'description', '示例 2'),
      JSON_OBJECT('input', '5\n5 4 -1 7 8',             'expected', '23', 'description', '示例 3')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static int maxSubArray(int[] nums) {\n        // TODO: Kadane 算法 O(n)\n        return 0;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] nums = new int[n];\n        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n        System.out.println(maxSubArray(nums));\n    }\n}',
    'python',
    'import sys\ninput = sys.stdin.readline\n\ndef max_sub_array(nums):\n    # TODO: Kadane 算法 O(n)\n    pass\n\nn = int(input())\nnums = list(map(int, input().split()))\nprint(max_sub_array(nums))',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\n\nint maxSubArray(vector<int>& nums) {\n    // TODO\n    return 0;\n}\n\nint main() {\n    int n; cin >> n;\n    vector<int> nums(n);\n    for (int& x : nums) cin >> x;\n    cout << maxSubArray(nums) << endl;\n    return 0;\n}'
  )
WHERE external_ref = 'Hot100-053';

-- ============================================================
-- Hot100-070: 爬楼梯
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '假设你正在爬楼梯，需要 `n` 阶才能到达楼顶。每次你可以爬 **1 或 2** 个台阶，问有多少种不同的方法可以爬到楼顶？\n\n---\n\n**输入格式**\n```\n一行整数 n（1 ≤ n ≤ 45）\n```\n\n**输出格式**\n```\n到达楼顶的方法数（整数）\n```\n\n**示例**\n```\n输入: 3\n输出: 3  （1+1+1, 1+2, 2+1）\n```\n\n**提示：** 斐波那契数列，时间复杂度 O(n)',
  judge_config = JSON_OBJECT(
    'inputFormat', '一行整数 n',
    'outputFormat', '方法数',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '2',  'expected', '2',  'description', '示例 1'),
      JSON_OBJECT('input', '3',  'expected', '3',  'description', '示例 2'),
      JSON_OBJECT('input', '10', 'expected', '89', 'description', '示例 3')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static int climbStairs(int n) {\n        // TODO: 动态规划（斐波那契数列）\n        return 0;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        System.out.println(climbStairs(n));\n    }\n}',
    'python',
    'import sys\n\ndef climb_stairs(n):\n    # TODO: 动态规划（斐波那契数列）\n    pass\n\nn = int(sys.stdin.readline())\nprint(climb_stairs(n))',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\n\nint climbStairs(int n) {\n    // TODO\n    return 0;\n}\n\nint main() {\n    int n; cin >> n;\n    cout << climbStairs(n) << endl;\n    return 0;\n}'
  )
WHERE external_ref = 'Hot100-070';

-- ============================================================
-- Hot100-104: 二叉树的最大深度（简化为数组层序表示）
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给定一个二叉树的**层序数组表示**（-1 代表 null 节点），找出其最大深度（根节点到最远叶节点的最长路径上的节点数）。\n\n---\n\n**输入格式**\n```\n第一行：节点数 n\n第二行：层序遍历数组（-1 代表空节点）\n```\n\n**输出格式**\n```\n最大深度（整数）\n```\n\n**示例**\n```\n输入:\n7\n3 9 20 -1 -1 15 7\n输出: 3\n\n输入:\n2\n1 -1\n输出: 2\n```\n\n**提示：** 递归 DFS 或迭代 BFS',
  judge_config = JSON_OBJECT(
    'inputFormat', '第一行：n\n第二行：层序数组（-1为null）',
    'outputFormat', '最大深度',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '7\n3 9 20 -1 -1 15 7', 'expected', '3', 'description', '示例 1'),
      JSON_OBJECT('input', '1\n1',                   'expected', '1', 'description', '示例 2 单节点'),
      JSON_OBJECT('input', '2\n1 -1',               'expected', '2', 'description', '示例 3 右空')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static class TreeNode {\n        int val;\n        TreeNode left, right;\n        TreeNode(int v) { val = v; }\n    }\n\n    static TreeNode buildTree(int[] arr) {\n        if (arr.length == 0 || arr[0] == -1) return null;\n        TreeNode root = new TreeNode(arr[0]);\n        Queue<TreeNode> q = new LinkedList<>();\n        q.offer(root);\n        int i = 1;\n        while (!q.isEmpty() && i < arr.length) {\n            TreeNode node = q.poll();\n            if (i < arr.length && arr[i] != -1) { node.left = new TreeNode(arr[i]); q.offer(node.left); }\n            i++;\n            if (i < arr.length && arr[i] != -1) { node.right = new TreeNode(arr[i]); q.offer(node.right); }\n            i++;\n        }\n        return root;\n    }\n\n    static int maxDepth(TreeNode root) {\n        // TODO: 递归 DFS\n        return 0;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] arr = new int[n];\n        for (int i = 0; i < n; i++) arr[i] = sc.nextInt();\n        System.out.println(maxDepth(buildTree(arr)));\n    }\n}',
    'python',
    'import sys\nfrom collections import deque\ninput = sys.stdin.readline\n\nclass TreeNode:\n    def __init__(self, v): self.val = v; self.left = self.right = None\n\ndef build_tree(arr):\n    if not arr or arr[0] == -1: return None\n    root = TreeNode(arr[0])\n    q = deque([root]); i = 1\n    while q and i < len(arr):\n        node = q.popleft()\n        if i < len(arr) and arr[i] != -1: node.left = TreeNode(arr[i]); q.append(node.left)\n        i += 1\n        if i < len(arr) and arr[i] != -1: node.right = TreeNode(arr[i]); q.append(node.right)\n        i += 1\n    return root\n\ndef max_depth(root):\n    # TODO\n    pass\n\nn = int(input())\narr = list(map(int, input().split()))\nprint(max_depth(build_tree(arr)))',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\nstruct TreeNode { int val; TreeNode *left, *right; TreeNode(int v):val(v),left(nullptr),right(nullptr){} };\nTreeNode* build(vector<int>& a) {\n    if (a.empty() || a[0]==-1) return nullptr;\n    TreeNode* root = new TreeNode(a[0]);\n    queue<TreeNode*> q; q.push(root); int i=1;\n    while (!q.empty() && i<(int)a.size()) {\n        auto node=q.front(); q.pop();\n        if (i<(int)a.size() && a[i]!=-1){node->left=new TreeNode(a[i]);q.push(node->left);} i++;\n        if (i<(int)a.size() && a[i]!=-1){node->right=new TreeNode(a[i]);q.push(node->right);} i++;\n    } return root;\n}\nint maxDepth(TreeNode* root) {\n    // TODO\n    return 0;\n}\nint main() {\n    int n; cin>>n; vector<int> a(n);\n    for(int& x:a) cin>>x;\n    cout<<maxDepth(build(a))<<endl;\n}'
  )
WHERE external_ref = 'Hot100-104';

-- ============================================================
-- Hot100-121: 买卖股票的最佳时机
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给定一个数组 `prices`，第 `i` 个元素是第 `i` 天的股票价格。你只能选择**某一天买入**，并在**之后某天卖出**，求最大利润。若不能获利，返回 0。\n\n---\n\n**输入格式**\n```\n第一行：天数 n\n第二行：n 个价格，空格分隔\n```\n\n**输出格式**\n```\n最大利润（整数）\n```\n\n**示例**\n```\n输入:\n6\n7 1 5 3 6 4\n输出: 5  （第2天买入1，第5天卖出6）\n```\n\n**提示：** 贪心算法，维护历史最低价，时间复杂度 O(n)',
  judge_config = JSON_OBJECT(
    'inputFormat', '第一行：n\n第二行：n 个价格',
    'outputFormat', '最大利润',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '6\n7 1 5 3 6 4', 'expected', '5', 'description', '示例 1'),
      JSON_OBJECT('input', '5\n7 6 4 3 1',   'expected', '0', 'description', '示例 2（下跌不获利）'),
      JSON_OBJECT('input', '3\n1 2 3',        'expected', '2', 'description', '示例 3（持续上涨）')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static int maxProfit(int[] prices) {\n        // TODO: 贪心，维护历史最低价\n        return 0;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] prices = new int[n];\n        for (int i = 0; i < n; i++) prices[i] = sc.nextInt();\n        System.out.println(maxProfit(prices));\n    }\n}',
    'python',
    'import sys\ninput = sys.stdin.readline\n\ndef max_profit(prices):\n    # TODO: 贪心，维护历史最低价\n    pass\n\nn = int(input())\nprices = list(map(int, input().split()))\nprint(max_profit(prices))',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\n\nint maxProfit(vector<int>& prices) {\n    // TODO\n    return 0;\n}\n\nint main() {\n    int n; cin >> n;\n    vector<int> p(n);\n    for (int& x : p) cin >> x;\n    cout << maxProfit(p) << endl;\n}'
  )
WHERE external_ref = 'Hot100-121';

-- ============================================================
-- Hot100-198: 打家劫舍
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '你是一个专业的小偷，计划偷窃沿街房屋。每间房内藏有现金，**相邻**的房屋装有相互连通的防盗系统，若**同晚**闯入**相邻**两间，系统会自动报警。给定数组 `nums` 代表各房屋金额，计算**不触动警报**下的最高金额。\n\n---\n\n**输入格式**\n```\n第一行：房屋数 n\n第二行：n 个金额，空格分隔\n```\n\n**输出格式**\n```\n可偷窃的最高金额\n```\n\n**示例**\n```\n输入:\n4\n1 2 3 1\n输出: 4  （偷第1+3间: 1+3=4）\n```\n\n**提示：** 动态规划，dp[i] = max(dp[i-1], dp[i-2]+nums[i])',
  judge_config = JSON_OBJECT(
    'inputFormat', '第一行：n\n第二行：n 个金额',
    'outputFormat', '最高金额',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '4\n1 2 3 1', 'expected', '4',  'description', '示例 1'),
      JSON_OBJECT('input', '4\n2 7 9 3', 'expected', '11', 'description', '示例 2（2+9=11）'),
      JSON_OBJECT('input', '1\n5',       'expected', '5',  'description', '示例 3（单间）')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static int rob(int[] nums) {\n        // TODO: 动态规划\n        // dp[i] = max(dp[i-1], dp[i-2] + nums[i])\n        return 0;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] nums = new int[n];\n        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n        System.out.println(rob(nums));\n    }\n}',
    'python',
    'import sys\ninput = sys.stdin.readline\n\ndef rob(nums):\n    # TODO: 动态规划\n    pass\n\nn = int(input())\nnums = list(map(int, input().split()))\nprint(rob(nums))',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\n\nint rob(vector<int>& nums) {\n    // TODO\n    return 0;\n}\n\nint main() {\n    int n; cin >> n;\n    vector<int> nums(n);\n    for (int& x : nums) cin >> x;\n    cout << rob(nums) << endl;\n}'
  )
WHERE external_ref = 'Hot100-198';

-- ============================================================
-- Hot100-300: 最长递增子序列
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给你一个整数数组 `nums`，找到其中**最长严格递增子序列**的长度。子序列是由数组派生而来的序列，**不要求连续**，但相对顺序不变。\n\n---\n\n**输入格式**\n```\n第一行：数组长度 n\n第二行：n 个整数，空格分隔\n```\n\n**输出格式**\n```\n最长递增子序列长度（整数）\n```\n\n**示例**\n```\n输入:\n8\n10 9 2 5 3 7 101 18\n输出: 4  （子序列 [2,3,7,18]）\n```\n\n**提示：** \n- DP O(n²)：dp[i] = max(dp[j]+1) for j<i and nums[j]<nums[i]\n- 二分搜索 O(n log n)',
  judge_config = JSON_OBJECT(
    'inputFormat', '第一行：n\n第二行：n 个整数',
    'outputFormat', '最长递增子序列长度',
    'timeLimit', 2000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '8\n10 9 2 5 3 7 101 18', 'expected', '4', 'description', '示例 1'),
      JSON_OBJECT('input', '4\n0 1 0 3',             'expected', '3', 'description', '示例 2'),
      JSON_OBJECT('input', '3\n7 7 7',               'expected', '1', 'description', '示例 3（全相同）')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static int lengthOfLIS(int[] nums) {\n        // TODO: DP O(n²) 或二分 O(n log n)\n        return 0;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] nums = new int[n];\n        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n        System.out.println(lengthOfLIS(nums));\n    }\n}',
    'python',
    'import sys\nimport bisect\ninput = sys.stdin.readline\n\ndef length_of_lis(nums):\n    # TODO: DP 或二分搜索\n    pass\n\nn = int(input())\nnums = list(map(int, input().split()))\nprint(length_of_lis(nums))',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\n\nint lengthOfLIS(vector<int>& nums) {\n    // TODO\n    return 0;\n}\n\nint main() {\n    int n; cin >> n;\n    vector<int> nums(n);\n    for (int& x : nums) cin >> x;\n    cout << lengthOfLIS(nums) << endl;\n}'
  )
WHERE external_ref = 'Hot100-300';

-- ============================================================
-- Hot100-322: 零钱兑换
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给你一个整数数组 `coins`，表示不同面额的硬币；以及一个整数 `amount`，表示总金额。计算并返回凑成总金额所需的**最少的硬币个数**。如果没有任何一种组合能组成总金额，返回 `-1`。每种硬币可以无限次使用。\n\n---\n\n**输入格式**\n```\n第一行：硬币种类数 n\n第二行：n 个面额，空格分隔\n第三行：目标金额 amount\n```\n\n**输出格式**\n```\n最少硬币数（不能凑成返回 -1）\n```\n\n**示例**\n```\n输入:\n3\n1 2 5\n11\n输出: 3  （5+5+1）\n```\n\n**提示：** 完全背包 DP，dp[i] = min(dp[i], dp[i-coin]+1)',
  judge_config = JSON_OBJECT(
    'inputFormat', '第一行：n\n第二行：n 个面额\n第三行：amount',
    'outputFormat', '最少硬币数（-1表示不可凑成）',
    'timeLimit', 2000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '3\n1 2 5\n11', 'expected', '3',  'description', '示例 1（5+5+1）'),
      JSON_OBJECT('input', '1\n2\n3',      'expected', '-1', 'description', '示例 2（不可凑成）'),
      JSON_OBJECT('input', '1\n1\n0',      'expected', '0',  'description', '示例 3（零金额）')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static int coinChange(int[] coins, int amount) {\n        // TODO: 完全背包 DP\n        // dp[i] = min(dp[i], dp[i-coin]+1)\n        return -1;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] coins = new int[n];\n        for (int i = 0; i < n; i++) coins[i] = sc.nextInt();\n        int amount = sc.nextInt();\n        System.out.println(coinChange(coins, amount));\n    }\n}',
    'python',
    'import sys\ninput = sys.stdin.readline\n\ndef coin_change(coins, amount):\n    # TODO: 完全背包 DP\n    pass\n\nn = int(input())\ncoins = list(map(int, input().split()))\namount = int(input())\nprint(coin_change(coins, amount))',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\n\nint coinChange(vector<int>& coins, int amount) {\n    // TODO\n    return -1;\n}\n\nint main() {\n    int n; cin >> n;\n    vector<int> coins(n);\n    for (int& c : coins) cin >> c;\n    int amount; cin >> amount;\n    cout << coinChange(coins, amount) << endl;\n}'
  )
WHERE external_ref = 'Hot100-322';

-- ============================================================
-- Hot100-206: 反转链表（简化为数组输入输出）
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给你单链表的头节点（以**数组形式**输入），请你反转链表，并以数组形式输出反转结果。\n\n---\n\n**输入格式**\n```\n第一行：节点数 n\n第二行：n 个节点值，空格分隔（按链表顺序）\n```\n\n**输出格式**\n```\n反转后的 n 个节点值，空格分隔\n```\n\n**示例**\n```\n输入:\n5\n1 2 3 4 5\n输出: 5 4 3 2 1\n```\n\n**提示：** 迭代法（prev/curr 双指针）或递归法，时间复杂度 O(n)',
  judge_config = JSON_OBJECT(
    'inputFormat', '第一行：n\n第二行：n 个节点值',
    'outputFormat', '反转后的值，空格分隔',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '5\n1 2 3 4 5', 'expected', '5 4 3 2 1', 'description', '示例 1'),
      JSON_OBJECT('input', '2\n1 2',        'expected', '2 1',       'description', '示例 2'),
      JSON_OBJECT('input', '1\n1',          'expected', '1',         'description', '示例 3（单节点）')
    )
  ),
  starter_code = JSON_OBJECT(
    'java',
    'import java.util.*;\n\npublic class Main {\n\n    static class ListNode {\n        int val;\n        ListNode next;\n        ListNode(int v) { val = v; }\n    }\n\n    static ListNode reverseList(ListNode head) {\n        // TODO: 迭代法（prev/curr）或递归法\n        return null;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        if (n == 0) return;\n        ListNode dummy = new ListNode(0), tail = dummy;\n        for (int i = 0; i < n; i++) {\n            tail.next = new ListNode(sc.nextInt());\n            tail = tail.next;\n        }\n        ListNode head = reverseList(dummy.next);\n        StringBuilder sb = new StringBuilder();\n        while (head != null) {\n            if (sb.length() > 0) sb.append(" ");\n            sb.append(head.val);\n            head = head.next;\n        }\n        System.out.println(sb);\n    }\n}',
    'python',
    'import sys\ninput = sys.stdin.readline\n\nclass ListNode:\n    def __init__(self, v): self.val = v; self.next = None\n\ndef reverse_list(head):\n    # TODO: 迭代法（prev/curr）\n    pass\n\nn = int(input())\nif n == 0:\n    print()\nelse:\n    vals = list(map(int, input().split()))\n    dummy = ListNode(0); tail = dummy\n    for v in vals:\n        tail.next = ListNode(v); tail = tail.next\n    head = reverse_list(dummy.next)\n    res = []\n    while head: res.append(str(head.val)); head = head.next\n    print(" ".join(res))',
    'cpp',
    '#include <bits/stdc++.h>\nusing namespace std;\nstruct ListNode { int val; ListNode* next; ListNode(int v):val(v),next(nullptr){} };\nListNode* reverseList(ListNode* head) {\n    // TODO\n    return nullptr;\n}\nint main() {\n    int n; cin >> n;\n    if (!n) return 0;\n    ListNode* dummy=new ListNode(0), *tail=dummy;\n    for (int i=0,v; i<n; i++){cin>>v; tail->next=new ListNode(v); tail=tail->next;}\n    ListNode* h=reverseList(dummy->next);\n    bool first=true;\n    while(h){if(!first)cout<<" ";cout<<h->val;first=false;h=h->next;}\n    cout<<endl;\n}'
  )
WHERE external_ref = 'Hot100-206';

-- ============================================================
-- Hot100-102: 二叉树的层序遍历
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给你二叉树的**层序数组表示**（-1 代表 null），返回层序遍历结果（不含 null，空格分隔）。\n\n---\n\n**输入格式**\n```\n第一行：节点数 n\n第二行：层序数组（-1 为 null）\n```\n\n**输出格式**\n```\n层序遍历值，空格分隔\n```\n\n**示例**\n```\n输入:\n7\n3 9 20 -1 -1 15 7\n输出: 3 9 20 15 7\n```',
  judge_config = JSON_OBJECT(
    'inputFormat', '第一行：n\n第二行：层序数组',
    'outputFormat', '层序值，空格分隔',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '7\n3 9 20 -1 -1 15 7', 'expected', '3 9 20 15 7', 'description', '示例 1'),
      JSON_OBJECT('input', '1\n1',                   'expected', '1',           'description', '示例 2'),
      JSON_OBJECT('input', '3\n1 -1 2',             'expected', '1 2',         'description', '示例 3')
    )
  ),
  starter_code = JSON_OBJECT(
    'java', 'import java.util.*;\n\npublic class Main {\n    static class TreeNode { int val; TreeNode left, right; TreeNode(int v){val=v;} }\n    static TreeNode build(int[] a) {\n        if (a.length==0||a[0]==-1) return null;\n        TreeNode root=new TreeNode(a[0]); Queue<TreeNode> q=new LinkedList<>(); q.offer(root); int i=1;\n        while(!q.isEmpty()&&i<a.length){ TreeNode n=q.poll();\n            if(i<a.length&&a[i]!=-1){n.left=new TreeNode(a[i]);q.offer(n.left);} i++;\n            if(i<a.length&&a[i]!=-1){n.right=new TreeNode(a[i]);q.offer(n.right);} i++; }\n        return root;\n    }\n    static String levelOrder(TreeNode root) {\n        // TODO: BFS 层序遍历\n        return "";\n    }\n    public static void main(String[] args) {\n        Scanner sc=new Scanner(System.in); int n=sc.nextInt(); int[] a=new int[n];\n        for(int i=0;i<n;i++) a[i]=sc.nextInt();\n        System.out.println(levelOrder(build(a)));\n    }\n}',
    'python', 'import sys\nfrom collections import deque\ninput=sys.stdin.readline\nclass TreeNode:\n    def __init__(self,v): self.val=v; self.left=self.right=None\ndef build(a):\n    if not a or a[0]==-1: return None\n    root=TreeNode(a[0]); q=deque([root]); i=1\n    while q and i<len(a):\n        node=q.popleft()\n        if i<len(a) and a[i]!=-1: node.left=TreeNode(a[i]); q.append(node.left)\n        i+=1\n        if i<len(a) and a[i]!=-1: node.right=TreeNode(a[i]); q.append(node.right)\n        i+=1\n    return root\ndef level_order(root):\n    pass\nn=int(input()); a=list(map(int,input().split()))\nprint(level_order(build(a)))',
    'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nstruct TreeNode{int val;TreeNode*l,*r;TreeNode(int v):val(v),l(nullptr),r(nullptr){}};\nstring levelOrder(TreeNode* root){return "";}\nint main(){int n;cin>>n;vector<int>a(n);for(int&x:a)cin>>x;/* build+print */}'
  )
WHERE external_ref = 'Hot100-102';

-- ============================================================
-- Hot100-141: 环形链表（简化输入）
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '给定链表节点数组，第三行给出**尾节点指向的下标**（-1 表示无环），判断是否有环。\n\n---\n\n**输入格式**\n```\n第一行：节点数 n\n第二行：n 个节点值\n第三行：cycle_pos（-1=无环）\n```\n\n**输出格式**\n```\ntrue 或 false\n```\n\n**示例**\n```\n输入:\n3\n1 2 3\n0\n输出: true\n```',
  judge_config = JSON_OBJECT(
    'inputFormat', '第一行：n\n第二行：节点值\n第三行：cycle_pos',
    'outputFormat', 'true 或 false',
    'timeLimit', 1000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '3\n1 2 3\n0',  'expected', 'true',  'description', '有环'),
      JSON_OBJECT('input', '3\n1 2 3\n-1', 'expected', 'false', 'description', '无环'),
      JSON_OBJECT('input', '1\n1\n-1',     'expected', 'false', 'description', '单节点无环')
    )
  ),
  starter_code = JSON_OBJECT(
    'java', 'import java.util.*;\n\npublic class Main {\n    static class ListNode { int val; ListNode next; ListNode(int v){val=v;} }\n    static boolean hasCycle(ListNode head) {\n        // TODO: 快慢指针\n        return false;\n    }\n    public static void main(String[] args) {\n        Scanner sc=new Scanner(System.in); int n=sc.nextInt();\n        ListNode[] nodes=new ListNode[n];\n        for(int i=0;i<n;i++) nodes[i]=new ListNode(sc.nextInt());\n        for(int i=0;i<n-1;i++) nodes[i].next=nodes[i+1];\n        int pos=sc.nextInt();\n        if(pos>=0&&pos<n) nodes[n-1].next=nodes[pos];\n        System.out.println(hasCycle(n==0?null:nodes[0]));\n    }\n}',
    'python', 'import sys\ninput=sys.stdin.readline\nclass ListNode:\n    def __init__(self,v): self.val=v; self.next=None\ndef has_cycle(head):\n    pass\nn=int(input()); vals=list(map(int,input().split())) if n else []\npos=int(input())\nnodes=[ListNode(v) for v in vals]\nfor i in range(n-1): nodes[i].next=nodes[i+1]\nif pos>=0: nodes[-1].next=nodes[pos]\nprint(str(has_cycle(nodes[0] if nodes else None)).lower())',
    'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nstruct ListNode{int val;ListNode*next;ListNode(int v):val(v),next(nullptr){}};\nbool hasCycle(ListNode* head){return false;}\nint main(){int n;cin>>n; /* build list */ return 0;}'
  )
WHERE external_ref = 'Hot100-141';

-- ============================================================
-- Hot100-146: LRU 缓存（简化操作序列）
-- ============================================================
UPDATE t_coding_challenge SET
  problem_md = '实现 LRU 缓存，按操作序列执行 PUT/GET，输出所有 GET 的结果（空格分隔，不存在返回 -1）。\n\n---\n\n**输入格式**\n```\n第一行：容量 capacity\n第二行：操作数 m\n接下来 m 行：PUT key value 或 GET key\n```\n\n**输出格式**\n```\n所有 GET 结果，空格分隔\n```\n\n**示例**\n```\n输入:\n2\n5\nPUT 1 1\nPUT 2 2\nGET 1\nPUT 3 3\nGET 2\n输出: 1 -1\n```',
  judge_config = JSON_OBJECT(
    'inputFormat', 'capacity + 操作序列',
    'outputFormat', 'GET 结果空格分隔',
    'timeLimit', 2000,
    'memoryLimit', 256,
    'testCases', JSON_ARRAY(
      JSON_OBJECT('input', '2\n5\nPUT 1 1\nPUT 2 2\nGET 1\nPUT 3 3\nGET 2', 'expected', '1 -1', 'description', '示例 1'),
      JSON_OBJECT('input', '1\n2\nPUT 2 1\nGET 2',                         'expected', '1',    'description', '示例 2'),
      JSON_OBJECT('input', '2\n4\nPUT 1 1\nPUT 2 2\nGET 1\nGET 3',        'expected', '1 -1', 'description', '示例 3')
    )
  ),
  starter_code = JSON_OBJECT(
    'java', 'import java.util.*;\n\npublic class Main {\n    static class LRUCache {\n        LRUCache(int cap) {}\n        int get(int key) { return -1; }\n        void put(int key, int val) {}\n    }\n    public static void main(String[] args) {\n        Scanner sc=new Scanner(System.in); int cap=sc.nextInt(), m=sc.nextInt();\n        LRUCache cache=new LRUCache(cap); StringBuilder sb=new StringBuilder();\n        for(int i=0;i<m;i++){ String op=sc.next();\n            if(op.equals("GET")){ int k=sc.nextInt(); int v=cache.get(k);\n                if(sb.length()>0) sb.append(" "); sb.append(v); }\n            else { int k=sc.nextInt(); int v=sc.nextInt(); cache.put(k, v); }\n        }\n        System.out.println(sb);\n    }\n}',
    'python', 'import sys\ninput=sys.stdin.readline\nclass LRUCache:\n    def __init__(self, cap): pass\n    def get(self, key): return -1\n    def put(self, key, val): pass\ncap=int(input()); m=int(input()); cache=LRUCache(cap); res=[]\nfor _ in range(m):\n    parts=input().split()\n    if parts[0]==\"GET\": res.append(str(cache.get(int(parts[1]))))\n    else: cache.put(int(parts[1]), int(parts[2]))\nprint(\" \".join(res))',
    'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nclass LRUCache{public: LRUCache(int c){} int get(int k){return -1;} void put(int k,int v){}};\nint main(){int cap,m;cin>>cap>>m; LRUCache c(cap); vector<int> res;\n    /* parse ops */ for(int x:res){cout<<x<<(x==res.back()?"":" ");} cout<<endl;}'
  )
WHERE external_ref = 'Hot100-146';
