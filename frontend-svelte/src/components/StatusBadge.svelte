<script>
  import { STATUS } from '../format.js'

  let { status } = $props()
  const info = $derived(STATUS[status] ?? { label: status, tone: 'neutral' })
</script>

<span class="badge badge-{info.tone}">{info.label}</span>
