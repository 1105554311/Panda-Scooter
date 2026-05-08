<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: '720px'
  },
  closable: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'close'])

const close = () => {
  if (!props.closable) {
    return
  }

  emit('update:modelValue', false)
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="fade-slide">
      <div v-if="modelValue" class="modal-root" @click.self="close">
        <div class="modal-card" :style="{ maxWidth: width }">
          <header class="modal-header">
            <div>
              <h3 class="modal-title">{{ title }}</h3>
              <p v-if="description" class="modal-description">{{ description }}</p>
            </div>
            <button v-if="closable" class="modal-close" type="button" @click="close">x</button>
          </header>
          <section class="modal-content">
            <slot />
          </section>
          <footer v-if="$slots.footer" class="modal-footer">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-root {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(11, 14, 13, 0.2);
}

.modal-card {
  width: min(100%, 100%);
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e5e2;
  background: #ffffff;
  overflow: hidden;
}

.modal-header,
.modal-footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  flex: 0 0 auto;
}

.modal-header {
  border-bottom: 1px solid #e5e5e2;
}

.modal-footer {
  border-top: 1px solid #e5e5e2;
}

.modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 400;
  letter-spacing: 0.12em;
}

.modal-description {
  margin: 8px 0 0;
  color: #737373;
  font-size: 14px;
}

.modal-content {
  flex: 1 1 auto;
  min-height: 0;
  padding: 20px 24px;
  overflow: auto;
}

.modal-close {
  width: 36px;
  height: 36px;
  border: 1px solid #d4d4d1;
  background: transparent;
  color: #0b0e0d;
  font-size: 14px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

@media (max-width: 720px) {
  .modal-root {
    padding: 16px;
  }

  .modal-header,
  .modal-content,
  .modal-footer {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
