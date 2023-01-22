<template id="news-list">
  <navbar active="news"></navbar>
  <p class="p-3" v-if="seeds.loading">Loading seeds...</p>
  <p class="p-3" v-if="seeds.loadError">Failed to load seeds! ({{ seeds.loadError.text }})</p>
  <seedbar :seeds="seeds"></seedbar>
  <main class="container" v-if="!seeds.loading">
    <seedcard v-for="seed in seeds.data" :seed="seed" :settings="settings"></seedcard>
  </main>
</template>
<script>
app.component("news-list", {
  template: "#news-list",
  data: function () {
    return {
      seeds: {
        loading: true,
        loadError: null,
        data: []
      },
      settings: loadSettings()
    }
  },
  created() {
    // the Javalin LoadableData helper class is not working correctly, state is not refreshed
    fetch("/api/seeds").then(res => {
      if (res.ok) return res.json();
      return res.text().then(text => { throw new Error(text) });
    })
      .then(data => this.seeds.data = data)
      .catch(error => this.seeds.loadError = { "text": error })
      .finally(() => this.seeds.loading = false);
  }
});
</script>
