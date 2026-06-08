export type UserRole = 'USER' | 'ADMIN'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  list: T[]
}

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatarUrl?: string
  targetPositionCode?: string
  role?: UserRole
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  userInfo: UserInfo
}

export interface Position {
  id: number
  code: string
  name: string
  description?: string
  techStack?: string[]
  iconUrl?: string
}

export interface ChatMessage {
  messageId?: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  messageType?: string
  questionId?: number
  questionOrder?: number
  questionType?: QuestionType
  questionTitle?: string
  topic?: string
  createdAt?: string
}

export type QuestionType = 'TECH_KNOWLEDGE' | 'SCENARIO' | 'PROJECT_DEEP' | 'BEHAVIOR'

export interface CodingChallenge {
  id: number
  title: string
  problemMd: string
  difficulty: number
  tags?: string[]
}

export interface CurrentQuestion {
  questionId: number
  questionOrder: number
  questionType: QuestionType
  questionTitle: string
  topic?: string
  codingChallenge?: CodingChallenge
}

export interface InterviewStartResult {
  sessionId: number
  positionCode: string
  positionName: string
  totalQuestions: number
  firstMessage: ChatMessage
  currentQuestion?: CurrentQuestion
}

export interface SseEvent {
  type: 'token' | 'done' | 'next_question' | 'interview_end' | 'error'
  content?: string
  messageId?: number
  messageType?: string
  questionId?: number
  questionOrder?: number
  questionType?: QuestionType
  questionTitle?: string
  topic?: string
  codingChallenge?: CodingChallenge
  reportId?: number
  message?: string
}

export interface ResumeStatus {
  resumeId: number
  parseStatus: 'PENDING' | 'SUCCESS' | 'FAILED'
  fileName?: string
  remark?: string
}

export interface ResumeProject {
  id: number
  projectName: string
  summaryMd?: string
  techStackTokens?: string[]
}

export interface ReportScores {
  tech: number
  expression: number
  logic: number
  depth: number
  confidence: number
}

export interface ReportDetail {
  reportId: number
  sessionId: number
  positionCode: string
  positionName: string
  reportStatus: 'GENERATING' | 'COMPLETED' | 'FAILED'
  overallScore?: number
  scores?: ReportScores
  summary?: string
  highlights?: string[]
  weaknesses?: string[]
  suggestions?: string[]
  questionScores?: unknown[]
}

export interface GrowthRecord {
  recordDate: string
  overallScore: number
  techScore: number
  expressionScore: number
  logicScore: number
  depthScore: number
  confidenceScore: number
  sessionId: number
  reportId: number
}

export interface GrowthData {
  positionCode?: string
  records: GrowthRecord[]
  trend?: {
    overallChange: number
    strongestDimension: string
    weakestDimension: string
  }
}

export interface KbTreeNode {
  id: number
  parentId?: number
  title: string
  nodeType: 'GROUP' | 'TOPIC_POINT'
  hasChildren?: boolean
  children?: KbTreeNode[]
}

export interface KbArticle {
  id: number
  title: string
  bodyMarkdown?: string
}

export interface KbNodeDetail {
  id: number
  title: string
  breadcrumb: { id: number; title: string }[]
  children: KbTreeNode[]
  articles: KbArticle[]
  bodyPreview?: string
}

export interface LearningResource {
  id: number
  title: string
  description?: string
  resourceType: string
  url?: string
  topic?: string
  positionCode?: string
}

export interface AdminStats {
  totalUsers: number
  totalInterviews: number
  totalReports: number
  todayInterviews: number
  activeUsers7d: number
}
