import apiClient from './client';

/**
 * Health check API — verifies backend connectivity.
 */
export const healthApi = {
  check: () => apiClient.get<string>('/health'),
};
