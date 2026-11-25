import { useCallback, useEffect, useState } from 'react'
import { useToast } from './toast.jsx'

/** Lädt Daten beim Einblenden und bietet reload(). Fehler erscheinen als Toast. */
export function useLoad(loader, initial = null) {
  const toast = useToast()
  const [data, setData] = useState(initial)
  const [loading, setLoading] = useState(true)

  const reload = useCallback(async () => {
    try {
      setData(await loader())
    } catch (e) {
      toast.error(e)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => { reload() }, [reload])
  return { data, loading, reload, setData }
}
