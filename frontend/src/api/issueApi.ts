import apiClient from './client';

export interface ImportTicketResponse {
  ticketId: string;
}

export interface ImportCompleteResponse {
  issueId: string;
}

export const issueApi = {
  /**
   * Upload a file for AI import processing.
   * Returns a ticketId used to open an SSE progress stream.
   */
  import: (file: File): Promise<ImportTicketResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    return apiClient
      .post<ImportTicketResponse>('/agents/import', formData, {
        headers: {
          'Content-Type': undefined,
        },
      })
      .then((res) => res.data as unknown as ImportTicketResponse);
  },
};
