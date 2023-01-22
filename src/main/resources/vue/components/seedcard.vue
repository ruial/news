<template id="seedcard">
  <div class="my-3 p-3 bg-body rounded shadow-sm" :id="seed.seedName.replaceAll(' ', '-')">
    <h5 class="border-bottom pb-2 mb-0">{{ seed.seedName }}</h5>

    <p class="pt-3" v-if="news.loading">Loading news...</p>
    <p class="pt-3" v-if="news.loadError">Failed to load news! ({{news.loadError.text}})</p>
    <p class="pt-3" v-if="!news.loading && !news.loadError && news.data.length === 0">No news to show.</p>
    <div class="d-flex text-muted pt-3 border-bottom" v-if="!news.loading" v-for="n in sortedNews">
      <p class="pb-3 mb-0 small lh-sm">
        <strong>{{ n.title }}</strong>
        <span class="d-block">{{ n.score }} points - {{ n.commentsCount }} <a :href="n.commentsUrl" rel="nofollow">comments</a></span>
        <a :href="`/news/${encodeURIComponent(n.id)}`">Preview</a> - {{ formatDate(n.date) }}
      </p>
    </div>
 
    <small class="d-block text-end mt-3">
      <a :href="seed.seedUrl">Homepage</a>
    </small>
  </div>
</template>
<script>
app.component("seedcard", {
  template: "#seedcard",
  props: ["seed", "settings"],
  data: function () {
    return {
      news: {
        loading: true,
        loadError: null,
        data: []
      }
    }
  },
  created() {
    const { maxCount, maxAgeDays } = this.settings;
    const params = buildQueryParams(maxCount, maxAgeDays); 
    fetch(`/api/seeds/${encodeURIComponent(this.seed.seedName)}?${params}`).then(res => {
      if (res.ok) return res.json();
      return res.text().then(text => { throw new Error(text) });
    })
      .then(data => this.news.data = data)
      .catch(error => this.news.loadError = { "text": error })
      .finally(() => this.news.loading = false);
  },
  computed: {
    sortedNews() {
      const sortBy = this.settings.sortBy;
      return [...this.news.data].sort((a, b) => b[sortBy] - a[sortBy]);
    }
  }
});
</script>
