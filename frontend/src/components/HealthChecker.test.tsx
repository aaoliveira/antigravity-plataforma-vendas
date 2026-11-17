import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { HealthChecker } from './HealthChecker';
import * as api from '@/lib/api';

describe('HealthChecker Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders loading state initially', () => {
    vi.spyOn(api, 'fetchHealth').mockImplementation(
      () => new Promise(() => {}) // never resolves
    );

    render(<HealthChecker />);
    expect(screen.getByText('Backend Health Status')).toBeDefined();
    expect(screen.getByText('VERIFICANDO')).toBeDefined();
  });

  it('renders UP status when backend is healthy', async () => {
    vi.spyOn(api, 'fetchHealth').mockResolvedValue({
      status: 'UP',
      components: {
        diskSpace: { status: 'UP' },
        ping: { status: 'UP' },
      },
    });

    render(<HealthChecker />);

    await waitFor(() => {
      expect(screen.getByText('UP')).toBeDefined();
      expect(screen.getByText('diskSpace: UP')).toBeDefined();
    });
  });

  it('renders DOWN status and displays error message when backend fails', async () => {
    vi.spyOn(api, 'fetchHealth').mockRejectedValue(new Error('Connection refused'));

    render(<HealthChecker />);

    await waitFor(() => {
      expect(screen.getByText('DOWN')).toBeDefined();
      expect(screen.getByText(/Connection refused/)).toBeDefined();
    });
  });
});
