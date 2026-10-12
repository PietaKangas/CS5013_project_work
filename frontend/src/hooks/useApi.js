import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';

/**
 * Loads JSON from the API when the component mounts or when `path` changes.
 *   const { data, loading, error, reload } = useApi('/documents/required');
 * Pass null as path to skip loading.
 */
export function useApi(path) {
  const [state, setState] = useState({ data: null, loading: path != null, error: null });
  const [reloadCount, setReloadCount] = useState(0);

  useEffect(() => {
    if (path == null) return undefined;
    const controller = new AbortController();
    setState((s) => ({ ...s, loading: true, error: null }));
    api(path, { signal: controller.signal })
      .then((data) => setState({ data, loading: false, error: null }))
      .catch((error) => {
        if (error.name !== 'AbortError') setState({ data: null, loading: false, error });
      });
    return () => controller.abort();
  }, [path, reloadCount]);

  const reload = useCallback(() => setReloadCount((n) => n + 1), []);
  return { ...state, reload };
}
