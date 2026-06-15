<template>
  <div class="coding-panel-root flex flex-col h-full min-h-0 rounded-xl border bg-[#1e1e1e] overflow-hidden">

    <!-- 顶部标题栏 -->
    <div class="flex items-center justify-between px-4 py-2.5 bg-[#252526] border-b border-[#3c3c3c] shrink-0">
      <div class="flex items-center gap-2 min-w-0">
        <span class="text-white font-semibold text-sm truncate">
          {{ challenge?.title || question?.questionTitle || '手撕代码' }}
        </span>
        <el-tag v-if="difficultyLabel" :type="difficultyType" size="small" effect="dark">
          {{ difficultyLabel }}
        </el-tag>
        <el-tag
          v-for="tag in (challenge?.tags || [])"
          :key="tag"
          size="small"
          effect="plain"
          class="!bg-[#3a3a3a] !border-[#555] !text-[#ccc] hidden md:inline-flex"
        >
          {{ tag }}
        </el-tag>
      </div>
      <div class="flex items-center gap-2 shrink-0">
        <el-select v-model="language" size="small" class="w-28 !text-xs" @change="onLanguageChange">
          <el-option label="Java"       value="java" />
          <el-option label="Python"     value="python" />
          <el-option label="C++"        value="cpp" />
          <el-option label="JavaScript" value="javascript" />
        </el-select>
        <el-tooltip content="重置为起始代码" placement="bottom">
          <el-button size="small" plain :icon="RefreshRight" @click="resetCode" class="!text-[#ccc] !bg-[#3a3a3a] !border-[#555]" />
        </el-tooltip>
      </div>
    </div>

    <!-- 主体：左（题目/测试）+ 右（编辑器） -->
    <div class="flex flex-1 min-h-0 overflow-hidden">

      <!-- 左栏：题目 + 测试用例 -->
      <div class="left-pane flex flex-col border-r border-[#3c3c3c] bg-[#1e1e1e]" :style="{ width: leftWidth }">
        <!-- 标签栏 -->
        <div class="flex border-b border-[#3c3c3c] shrink-0">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="px-4 py-2 text-xs font-medium transition-colors"
            :class="activeTab === tab.key
              ? 'text-white border-b-2 border-[#569cd6] bg-[#252526]'
              : 'text-[#969696] hover:text-white'"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 题目描述 -->
        <div v-if="activeTab === 'problem'" class="flex-1 overflow-y-auto p-4 text-[#d4d4d4] text-sm leading-relaxed">
          <div v-if="challenge" v-html="renderedProblemMd" class="prose-dark" />
          <div v-else class="text-[#969696]">
            <p class="font-medium text-white mb-2">{{ question?.questionTitle }}</p>
            <p>请在右侧编辑器中编写你的解答代码，并点击「运行示例」验证输出，确认无误后点击「提交代码」。</p>
          </div>
          <!-- 输入/输出格式 -->
          <template v-if="judgeConfig?.inputFormat || judgeConfig?.outputFormat">
            <hr class="border-[#3c3c3c] my-3" />
            <div v-if="judgeConfig?.inputFormat">
              <p class="text-[#569cd6] font-semibold mb-1">输入格式</p>
              <pre class="bg-[#2d2d2d] rounded p-2 text-xs whitespace-pre-wrap">{{ judgeConfig.inputFormat }}</pre>
            </div>
            <div v-if="judgeConfig?.outputFormat" class="mt-2">
              <p class="text-[#569cd6] font-semibold mb-1">输出格式</p>
              <pre class="bg-[#2d2d2d] rounded p-2 text-xs whitespace-pre-wrap">{{ judgeConfig.outputFormat }}</pre>
            </div>
          </template>
        </div>

        <!-- 测试用例 -->
        <div v-if="activeTab === 'testcases'" class="flex-1 overflow-y-auto p-3 space-y-2">
          <div v-if="!visibleTestCases.length" class="text-[#969696] text-sm p-2">
            暂无测试用例，请直接编写代码并运行
          </div>
          <div
            v-for="(tc, i) in visibleTestCases"
            :key="i"
            class="rounded border border-[#3c3c3c] bg-[#252526] p-3 text-xs"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="text-[#569cd6] font-semibold">{{ tc.description || '测试 ' + (i + 1) }}</span>
              <el-tag
                v-if="tc._result !== undefined"
                :type="tc._result ? 'success' : 'danger'"
                size="small"
                effect="dark"
              >
                {{ tc._result ? '✓ 通过' : '✗ 失败' }}
              </el-tag>
            </div>
            <div>
              <span class="text-[#969696]">输入：</span>
              <pre class="mt-1 bg-[#1e1e1e] rounded p-1.5 text-[#ce9178] whitespace-pre-wrap text-xs">{{ tc.input }}</pre>
            </div>
            <div class="mt-1">
              <span class="text-[#969696]">期望输出：</span>
              <pre class="mt-1 bg-[#1e1e1e] rounded p-1.5 text-[#b5cea8] whitespace-pre-wrap text-xs">{{ tc.expected }}</pre>
            </div>
            <div v-if="tc._actual !== undefined" class="mt-1">
              <span class="text-[#969696]">实际输出：</span>
              <pre
                class="mt-1 rounded p-1.5 whitespace-pre-wrap text-xs"
                :class="tc._result ? 'bg-[#1e1e1e] text-[#b5cea8]' : 'bg-[#3c1f1f] text-[#f48771]'"
              >{{ tc._actual || '(空)' }}</pre>
            </div>
          </div>
        </div>

        <!-- 提交历史 -->
        <div v-if="activeTab === 'history'" class="flex-1 overflow-y-auto p-3 space-y-2">
          <div v-if="!submitHistory.length" class="text-[#969696] text-sm p-2">暂无提交记录</div>
          <div
            v-for="(h, i) in submitHistory"
            :key="i"
            class="rounded border border-[#3c3c3c] bg-[#252526] p-3 text-xs cursor-pointer hover:border-[#569cd6] transition-colors"
            @click="loadHistoryCode(h)"
          >
            <div class="flex items-center justify-between">
              <span class="text-[#ccc]">第 {{ h.submitOrder }} 次 · {{ h.language }}</span>
              <el-tag
                v-if="h.runStatus"
                :type="h.runStatus === 'PASSED' ? 'success' : h.runStatus === 'ERROR' ? 'danger' : 'warning'"
                size="small"
                effect="dark"
              >
                {{ runStatusLabel(h.runStatus) }}
              </el-tag>
            </div>
            <div v-if="h.testsPassed !== undefined && h.testsTotal" class="mt-1 text-[#969696]">
              {{ h.testsPassed }}/{{ h.testsTotal }} 测试通过
            </div>
          </div>
        </div>
      </div>

      <!-- 右栏：Monaco 编辑器 -->
      <div class="flex flex-col flex-1 min-h-0 min-w-0">
        <VueMonacoEditor
          v-model:value="code"
          :language="monacoLanguage"
          theme="vs-dark"
          :options="editorOptions"
          class="flex-1 min-h-0"
        />

        <!-- 底部控制台 -->
        <div class="console-area border-t border-[#3c3c3c] bg-[#1e1e1e] shrink-0" :class="{ 'console-open': showConsole }">
          <!-- 操作栏 -->
          <div class="flex items-center justify-between px-3 py-1.5 bg-[#252526]">
            <div class="flex items-center gap-2">
              <button
                class="text-xs text-[#969696] hover:text-white flex items-center gap-1 transition-colors"
                @click="showConsole = !showConsole"
              >
                <span>控制台</span>
                <span class="text-[10px]">{{ showConsole ? '▼' : '▲' }}</span>
              </button>
              <el-tag
                v-if="lastRunStatus"
                :type="lastRunStatus === 'PASSED' ? 'success' : lastRunStatus === 'ERROR' ? 'danger' : 'warning'"
                size="small"
                effect="dark"
                class="text-[10px]"
              >
                {{ runStatusLabel(lastRunStatus) }}
              </el-tag>
              <span v-if="lastPassRatio" class="text-[#969696] text-xs">
                {{ lastPassRatio }}
              </span>
            </div>
            <div class="flex gap-2">
              <el-button
                size="small"
                plain
                :loading="running"
                :disabled="!code.trim() || submitting"
                @click="runCode"
                class="!text-[#ccc] !bg-[#3a3a3a] !border-[#555] text-xs"
              >
                运行示例
              </el-button>
              <el-button
                size="small"
                type="primary"
                :loading="submitting"
                :disabled="!code.trim() || running"
                @click="submitCode"
              >
                提交代码
              </el-button>
            </div>
          </div>

          <!-- 控制台输出 -->
          <transition name="slide-down">
            <div v-if="showConsole" class="console-output px-3 py-2 overflow-y-auto bg-[#1e1e1e]">
              <div v-if="consoleLines.length === 0" class="text-[#969696] text-xs">运行代码后，输出将显示在此处...</div>
              <div v-for="(line, i) in consoleLines" :key="i">
                <pre
                  class="text-xs whitespace-pre-wrap font-mono"
                  :class="{
                    'text-[#f48771]': line.type === 'error',
                    'text-[#b5cea8]': line.type === 'success',
                    'text-[#569cd6]': line.type === 'info',
                    'text-[#d4d4d4]': line.type === 'stdout',
                  }"
                >{{ line.text }}</pre>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'
import { ElMessage } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import { marked } from 'marked'
import * as interviewApi from '@/api/interview'
import request from '@/utils/request'

interface TestCase {
  input: string
  expected: string
  description?: string
  _result?: boolean
  _actual?: string
}

interface JudgeConfig {
  testCases?: TestCase[]
  inputFormat?: string
  outputFormat?: string
  timeLimit?: number
}

interface ChallengeDetail {
  id: number
  externalRef?: string
  title: string
  problemMd?: string
  difficulty?: number
  tags?: string[]
  judgeConfig?: JudgeConfig
  starterCode?: Record<string, string>
}

interface SubmitHistory {
  submitOrder: number
  language: string
  runStatus?: string
  testsPassed?: number
  testsTotal?: number
  code?: string
  createdAt?: string
}

const props = defineProps<{
  question: {
    questionId: number
    questionTitle: string
    questionType: string
    codingChallenge?: {
      id: number
      title: string
      problemMd: string
      difficulty?: number
      tags?: string[]
    }
  } | null
  sessionId: number
}>()

const emit = defineEmits<{
  (e: 'submitted', payload: { submitOrder: number; runStatus: string; testsPassed: number; testsTotal: number }): void
}>()

const language     = ref('java')
const code         = ref('')
const running      = ref(false)
const submitting   = ref(false)
const showConsole  = ref(false)
const activeTab    = ref<'problem' | 'testcases' | 'history'>('problem')
const challenge    = ref<ChallengeDetail | null>(null)
const consoleLines = ref<{ type: string; text: string }[]>([])
const lastRunStatus = ref('')
const lastPassRatio = ref('')
const submitHistory = ref<SubmitHistory[]>([])
const leftWidth    = ref('38%')

const tabs: { key: 'problem' | 'testcases' | 'history'; label: string }[] = [
  { key: 'problem',   label: '题目' },
  { key: 'testcases', label: '测试用例' },
  { key: 'history',   label: '提交记录' },
]

const editorOptions = {
  fontSize: 13,
  minimap: { enabled: false },
  scrollBeyondLastLine: false,
  lineNumbers: 'on' as const,
  tabSize: 4,
  insertSpaces: true,
  wordWrap: 'off' as const,
  automaticLayout: true,
}

const monacoLanguage = computed(() => {
  const map: Record<string, string> = {
    java: 'java', python: 'python', cpp: 'cpp', javascript: 'javascript',
  }
  return map[language.value] || 'java'
})

const judgeConfig = computed<JudgeConfig | null>(() => challenge.value?.judgeConfig ?? null)

const visibleTestCases = computed<TestCase[]>(() => judgeConfig.value?.testCases ?? [])

const difficultyLabel = computed(() => {
  const d = challenge.value?.difficulty
  if (d === 1) return '简单'
  if (d === 2) return '中等'
  if (d === 3) return '困难'
  return ''
})

const difficultyType = computed(() => {
  const d = challenge.value?.difficulty
  if (d === 1) return 'success'
  if (d === 2) return 'warning'
  if (d === 3) return 'danger'
  return 'info'
})

const renderedProblemMd = computed(() => {
  const md = challenge.value?.problemMd
  if (!md) return ''
  return marked.parse(md, { async: false }) as string
})

function runStatusLabel(status: string) {
  const map: Record<string, string> = {
    PASSED: '全部通过', FAILED: '未通过', ERROR: '运行错误', TIMEOUT: '超时', PENDING: '待评判',
  }
  return map[status] || status
}

function getStarterCode() {
  const sc = challenge.value?.starterCode
  if (sc && sc[language.value]) return sc[language.value]
  return getDefaultStarterCode()
}

function getDefaultStarterCode() {
  const title = props.question?.questionTitle || '题目'
  const templates: Record<string, string> = {
    java: `import java.util.*;\n\npublic class Main {\n\n    // ${title}\n    static Object solve() {\n        // 在这里实现你的解法\n        return null;\n    }\n\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // 读取输入，调用 solve()，输出结果\n    }\n}`,
    python: `import sys\ninput = sys.stdin.readline\n\n# ${title}\ndef solve():\n    # 在这里实现你的解法\n    pass\n\nsolve()`,
    cpp: `#include <bits/stdc++.h>\nusing namespace std;\n\n// ${title}\nint main() {\n    // 在这里实现你的解法\n    return 0;\n}`,
    javascript: `// ${title}\nconst lines = require('fs').readFileSync('/dev/stdin', 'utf8').trim().split('\\n');\n\nfunction solve() {\n    // 在这里实现你的解法\n}\n\nconsole.log(solve());`,
  }
  return templates[language.value] || templates.java
}

function onLanguageChange() {
  const sc = challenge.value?.starterCode
  if (sc && sc[language.value]) {
    code.value = sc[language.value]
  } else {
    code.value = getDefaultStarterCode()
  }
}

function resetCode() {
  code.value = getStarterCode()
}

function appendConsole(type: string, text: string) {
  consoleLines.value.push({ type, text })
  showConsole.value = true
}

function clearConsole() {
  consoleLines.value = []
}

async function runCode() {
  if (!code.value.trim() || running.value) return
  running.value = true
  clearConsole()
  appendConsole('info', '▶ 运行示例输入...')

  try {
    const res = await request.post<unknown, Record<string, unknown>>('/coding/run', {
      challengeId: props.question?.codingChallenge?.id ?? null,
      language: language.value,
      code: code.value,
      mode: 'run',
    })

    if (res.error && String(res.error).trim()) {
      appendConsole('error', `✗ 错误：${res.error}`)
      if (res.stderr) appendConsole('error', String(res.stderr))
      lastRunStatus.value = 'ERROR'
    } else {
      const stdout = String(res.stdout || '').trim()
      const stderr = String(res.stderr || '').trim()
      if (stdout) appendConsole('stdout', stdout)
      if (stderr) appendConsole('error', stderr)
      if (!stdout && !stderr) appendConsole('info', '（无输出）')

      const sampleInput = String(res.sampleInput || '')
      if (sampleInput) appendConsole('info', `\n示例输入：\n${sampleInput}`)
      lastRunStatus.value = ''
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    appendConsole('error', `✗ 请求失败：${msg}`)
  } finally {
    running.value = false
  }
}

async function submitCode() {
  if (!code.value.trim() || submitting.value) return
  if (!props.question?.codingChallenge?.id) {
    ElMessage.warning('当前题目暂无测试用例配置，请用「运行示例」验证后向面试官说明思路')
    return
  }
  submitting.value = true
  clearConsole()
  appendConsole('info', '⬆ 正在提交，运行全部测试用例...')

  try {
    const res = await request.post<unknown, Record<string, unknown>>('/coding/run', {
      challengeId: props.question.codingChallenge.id,
      sessionId: props.sessionId,
      questionId: props.question.questionId,
      language: language.value,
      code: code.value,
      mode: 'submit',
    })

    const passed  = Number(res.passed  ?? 0)
    const total   = Number(res.total   ?? 0)
    const status  = String(res.runStatus || 'PENDING')
    lastRunStatus.value = status
    lastPassRatio.value = total > 0 ? `${passed}/${total} 通过` : ''

    if (status === 'PASSED') {
      appendConsole('success', `✓ 全部 ${total} 个测试用例通过！`)
    } else {
      appendConsole('error', `✗ ${passed}/${total} 个测试用例通过`)
    }

    // 更新测试用例面板的结果
    const testResults = (res.testResults as Array<Record<string, unknown>>) || []
    testResults.forEach((r) => {
      const idx = (Number(r.index) || 1) - 1
      const tc = visibleTestCases.value[idx]
      if (tc) {
        tc._result = Boolean(r.passed)
        tc._actual = String(r.actual || '')
        if (!r.passed && r.error) appendConsole('error', `  测试 ${r.index}: ${r.error}`)
      }
    })

    if (total > 0) activeTab.value = 'testcases'

    const stderr = String(res.stderr || '').trim()
    if (stderr) appendConsole('error', `\n错误信息：\n${stderr}`)

    emit('submitted', { submitOrder: 1, runStatus: status, testsPassed: passed, testsTotal: total })
    await loadHistory()
    ElMessage[status === 'PASSED' ? 'success' : 'warning'](
      status === 'PASSED' ? `${total} 个测试全部通过！` : `${passed}/${total} 个测试通过`
    )
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    appendConsole('error', `✗ 提交失败：${msg}`)
  } finally {
    submitting.value = false
  }
}

async function loadChallenge() {
  const cid = props.question?.codingChallenge?.id
  if (!cid) {
    challenge.value = null
    code.value = getDefaultStarterCode()
    return
  }
  try {
    const res = await request.get<unknown, ChallengeDetail>(`/coding/${cid}`)
    challenge.value = res
    code.value = res.starterCode?.[language.value] ?? getDefaultStarterCode()
  } catch {
    challenge.value = null
    code.value = getDefaultStarterCode()
  }
}

async function loadHistory() {
  if (!props.sessionId || !props.question?.questionId) return
  try {
    const res = await interviewApi.getLatestCodingSubmit(props.sessionId, props.question.questionId)
    if (res.submitted) {
      submitHistory.value = [{
        submitOrder: res.submitOrder || 1,
        language: res.language || 'java',
        code: res.code,
        createdAt: res.createdAt,
      }]
    }
  } catch { /* ignore */ }
}

function loadHistoryCode(h: SubmitHistory) {
  if (h.code) {
    code.value = h.code
    language.value = h.language || language.value
    ElMessage.info('已加载该次提交的代码')
  }
}

watch(
  () => props.question?.questionId,
  async () => {
    clearConsole()
    lastRunStatus.value = ''
    lastPassRatio.value = ''
    submitHistory.value = []
    activeTab.value = 'problem'
    await loadChallenge()
    await loadHistory()
  },
  { immediate: true },
)

onMounted(() => {
  /* 布局初始化由父组件 CSS 控制 */
})
</script>

<style scoped>
.coding-panel-root {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
}

.left-pane {
  min-width: 220px;
  max-width: 45%;
}

/* 控制台展开/收起高度 */
.console-area {
  max-height: 40px;
  transition: max-height 0.2s ease;
}
.console-area.console-open {
  max-height: 220px;
}

.console-output {
  max-height: 180px;
}

/* Markdown 暗色主题 */
.prose-dark :deep(h1),
.prose-dark :deep(h2),
.prose-dark :deep(h3) {
  color: #d4d4d4;
  margin: 0.5em 0 0.3em;
}
.prose-dark :deep(p)   { margin: 0.4em 0; }
.prose-dark :deep(code) {
  background: #2d2d2d;
  color: #ce9178;
  padding: 0.1em 0.35em;
  border-radius: 3px;
  font-size: 0.92em;
}
.prose-dark :deep(pre) {
  background: #2d2d2d;
  padding: 0.75em;
  border-radius: 6px;
  overflow-x: auto;
}
.prose-dark :deep(pre code) {
  background: transparent;
  color: #d4d4d4;
  padding: 0;
}
.prose-dark :deep(strong) { color: #d7ba7d; }
.prose-dark :deep(hr) { border-color: #3c3c3c; }
.prose-dark :deep(ul),
.prose-dark :deep(ol) {
  padding-left: 1.4em;
  margin: 0.3em 0;
}
.prose-dark :deep(li) { margin: 0.15em 0; }

/* 控制台滑入动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}
.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
