<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  const sessionId = route.params.sessionId as string | undefined
  if (sessionId) {
    router.replace({ name: 'Consult', params: { sessionId } })
  } else {
    router.replace({ name: 'Consult' })
  }
})
</script>

<template>
  <div class="redirect-placeholder" />
</template>

<style scoped>
.redirect-placeholder {
  min-height: 100vh;
}
</style>
