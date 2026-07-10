import { create } from 'zustand';

interface AppState {
  /** Current authenticated user info (null = not logged in) */
  user: { id: string; username: string } | null;
  setUser: (user: AppState['user']) => void;

  /** Sidebar collapsed state */
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
}

export const useAppStore = create<AppState>((set) => ({
  user: null,
  setUser: (user) => set({ user }),

  sidebarCollapsed: false,
  toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
}));
