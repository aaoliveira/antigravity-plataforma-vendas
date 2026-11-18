'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';

export interface UserMembership {
  tenantId: string;
  tenantName: string;
  role: string;
  status: string;
}

export interface UserProfile {
  userId: string;
  email: string;
  fullName: string;
  status: string;
  memberships: UserMembership[];
}

export interface CurrentTenant {
  tenantId: string;
  name: string;
  slug: string;
  role: string;
  status: string;
}

interface AuthContextType {
  user: UserProfile | null;
  currentTenant: CurrentTenant | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  sessionExpired: boolean;
  login: (email: string, password: string, requestedTenantId?: string) => Promise<void>;
  logout: () => Promise<void>;
  switchTenant: (targetTenantId: string) => Promise<void>;
  refreshSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [currentTenant, setCurrentTenant] = useState<CurrentTenant | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [sessionExpired, setSessionExpired] = useState<boolean>(false);

  const refreshSession = useCallback(async () => {
    try {
      const res = await fetch('/api/auth/session', { cache: 'no-store' });
      if (res.ok) {
        const data = await res.json();
        if (data.authenticated) {
          setUser(data.user);
          setCurrentTenant(data.currentTenant);
          setSessionExpired(false);
        } else {
          setUser(null);
          setCurrentTenant(null);
        }
      } else if (res.status === 401) {
        const errData = await res.json().catch(() => ({}));
        if (errData.error === 'SESSION_EXPIRED') {
          setSessionExpired(true);
        }
        setUser(null);
        setCurrentTenant(null);
      } else {
        setUser(null);
        setCurrentTenant(null);
      }
    } catch (e) {
      setUser(null);
      setCurrentTenant(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshSession();
  }, [refreshSession]);

  const login = async (email: string, password: string, requestedTenantId?: string) => {
    setIsLoading(true);
    setSessionExpired(false);
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, requestedTenantId }),
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.detail || data.title || 'Falha ao autenticar.');
      }

      await refreshSession();
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } finally {
      setUser(null);
      setCurrentTenant(null);
      setSessionExpired(false);
      window.location.href = '/login';
    }
  };

  const switchTenant = async (targetTenantId: string) => {
    const res = await fetch('/api/auth/switch-tenant', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ targetTenantId }),
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.detail || data.message || 'Falha ao alternar organização.');
    }

    await refreshSession();
  };

  const value: AuthContextType = {
    user,
    currentTenant,
    isLoading,
    isAuthenticated: !!user,
    sessionExpired,
    login,
    logout,
    switchTenant,
    refreshSession,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    return {
      user: null,
      currentTenant: null,
      isLoading: false,
      isAuthenticated: false,
      sessionExpired: false,
      login: async () => {},
      logout: async () => {},
      switchTenant: async () => {},
      refreshSession: async () => {},
    };
  }
  return context;
}
