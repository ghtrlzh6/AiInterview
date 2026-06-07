/** 将后端返回的 /uploads/... 路径转为可访问 URL（开发环境走 Vite 代理） */
export function resolveUploadUrl(url?: string | null): string {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/') ? url : `/${url}`
}
