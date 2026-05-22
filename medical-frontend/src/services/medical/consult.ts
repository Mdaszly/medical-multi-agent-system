import type { ConsultStreamEvent, ConsultVORaw } from './consultTypes'
import { mapConsultVo } from './consultTypes'

export { createSession, listMessages, deleteSession, queryHistory, saveMessage, updateTitle } from './xianshangwenzhenhuihua'

export type { ConsultResult, ConsultStreamEvent, GraphEvidence, SymptomMatch } from './consultTypes'
export { mapConsultVo } from './consultTypes'

const getBaseUrl = () => {
  if (import.meta.env.DEV) {
    return ''
  }
  return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
}

/**
 * 流式问诊 SSE：chunk → content，done → 映射后的 ConsultResult（含图谱命中状态）
 */
export async function consultStreamWithGraph(
  body: { question: string; sessionId?: string; patientContext?: Record<string, unknown>; scene?: string },
  onMessage: (event: ConsultStreamEvent) => void,
  onError?: (error: Error) => void
): Promise<void> {
  const token = localStorage.getItem('satoken')
  const url = `${getBaseUrl()}/api/v1/consult/stream`

  let response: Response
  try {
    response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: token } : {}),
      },
      body: JSON.stringify(body),
    })
  } catch (e) {
    const err = e instanceof Error ? e : new Error('网络连接失败')
    onError?.(err)
    throw err
  }

  if (!response.ok) {
    const err = new Error(`问诊请求失败 (${response.status})`)
    onError?.(err)
    throw err
  }

  const reader = response.body?.getReader()
  if (!reader) {
    const err = new Error('无法读取流式响应')
    onError?.(err)
    throw err
  }

  const decoder = new TextDecoder()
  let buffer = ''
  let currentEvent = ''

  const processLine = (line: string) => {
    if (line.startsWith('event:')) {
      currentEvent = line.slice(6).trim()
      return
    }
    if (!line.startsWith('data:')) {
      return
    }
    const data = line.slice(5).trim()
    if (!data) {
      return
    }

    if (currentEvent === 'chunk' || currentEvent === '') {
      try {
        const parsed = JSON.parse(data)
        const text = typeof parsed === 'string' ? parsed : String(parsed)
        onMessage({ type: 'content', content: text })
      } catch {
        onMessage({ type: 'content', content: data })
      }
    } else if (currentEvent === 'done') {
      try {
        const raw = JSON.parse(data) as ConsultVORaw
        onMessage({ type: 'done', data: mapConsultVo(raw) })
      } catch (e) {
        onError?.(e instanceof Error ? e : new Error('解析问诊结果失败'))
      }
    } else if (currentEvent === 'error') {
      let message = data
      try {
        message = JSON.parse(data)
      } catch {
        /* use raw */
      }
      onMessage({ type: 'error', error: String(message) })
    }
  }

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() ?? ''
      for (const line of lines) {
        const trimmed = line.replace(/\r$/, '')
        if (trimmed === '') {
          currentEvent = ''
          continue
        }
        processLine(trimmed)
      }
    }
    if (buffer.trim()) {
      for (const line of buffer.split('\n')) {
        processLine(line.replace(/\r$/, ''))
      }
    }
  } catch (e) {
    const err = e instanceof Error ? e : new Error('流式读取中断')
    onError?.(err)
    throw err
  }
}

/** 默认导出：带图谱映射的流式问诊（覆盖 code-generated 桩） */
export { consultStreamWithGraph as consultStream }
