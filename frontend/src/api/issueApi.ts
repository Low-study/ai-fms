export interface ImportTicketResponse {
  ticketId: string;
}

export interface SseEvent {
  type: string;
  data: string;
}

/**
 * 上传文件到 Agent 导入接口，返回 SSE 事件流。
 * 后端返回 text/event-stream（SSE），不能用 Axios（会尝试解 JSON 失败）。
 * 使用原生 fetch 读取 ReadableStream 解析 SSE。
 */
export const issueApi = {
  importStream: (file: File): Promise<ReadableStreamDefaultReader<Uint8Array>> => {
    const formData = new FormData();
    formData.append('file', file);

    return fetch('/api/v1/agents/import', {
      method: 'POST',
      body: formData,
    }).then((res) => {
      if (!res.ok) {
        return res.json().then((err) => {
          throw new Error(err.message || `HTTP ${res.status}`);
        });
      }
      if (!res.body) {
        throw new Error('No response body');
      }
      return res.body.getReader();
    });
  },

  /**
   * SSE 文本流 → 事件流
   */
  parseSseStream: async function* (reader: ReadableStreamDefaultReader<Uint8Array>): AsyncGenerator<SseEvent> {
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      let eventType = 'message';
      let data = '';

      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.slice(6).trim();
        } else if (line.startsWith('data:')) {
          data = line.slice(5).trim();
        } else if (line === '' && data) {
          yield { type: eventType, data };
          eventType = 'message';
          data = '';
        }
      }
    }
  },
};
