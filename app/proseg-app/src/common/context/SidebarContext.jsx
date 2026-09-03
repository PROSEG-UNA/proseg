import { createContext, useState } from 'react';

export const SidebarContext = createContext({
  isMinimized: false,
  setIsMinimized: () => {},
});

export function SidebarProvider({ children }) {
  const [isMinimized, setIsMinimized] = useState(false);

  return (
    <SidebarContext.Provider value={{ isMinimized, setIsMinimized }}>
      {children}
    </SidebarContext.Provider>
  );
}
