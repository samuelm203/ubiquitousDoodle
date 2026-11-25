<script>
  let { title, onclose, children } = $props()
</script>

<svelte:window onkeydown={(e) => e.key === 'Escape' && onclose()} />

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="modal-backdrop" onmousedown={(e) => e.target === e.currentTarget && onclose()}>
  <div class="modal" role="dialog" aria-modal="true" aria-label={title}>
    <header class="modal-head">
      <h2>{title}</h2>
      <button class="btn btn-ghost btn-icon" aria-label="Schliessen" onclick={onclose}>✕</button>
    </header>
    {@render children()}
  </div>
</div>
