<template id="recommendations">
  <navbar active="recommendations"></navbar>
  <main class="container">
    <h3 class="mt-4 mb-3">Recommendations</h3>
    <p>Based on your favorite news</p>
    <p class="py-3" v-if="recos.loading">Loading recommendations...</p>
    <p class="py-3" v-if="recos.loadError">Failed to load recommendations! ({{ recos.loadError.text }})</p>
    <p class="py-3" v-if="!recos.loading && !recos.loadError && recos.data.length === 0">No recommendations to show.</p>
    <searchresults :results="recos.data" v-if="!recos.loading && recos.data.length > 0"></searchresults>
  </main>
</template>
<script>
app.component("recommendations", {
  template: "#recommendations",
  data: function () {
    return {
      recos: {
        loading: true,
        loadError: null,
        data: []
      },
      settings: loadSettings(),
      favorites: loadFavorites()
    }
  },
  created() {
    const { maxCountSearch, maxAgeDaysSearch } = this.settings;
    const params = buildQueryParams(maxCountSearch, maxAgeDaysSearch); 
    fetch(`/api/recommendations/news?${params}`, {
      method: "POST",
      headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
      },
      body: JSON.stringify(this.favorites.map(n => n.id))
    }).then(res => {
      if (res.ok) return res.json();
      return res.text().then(text => { throw new Error(text) });
    })
      .then(data => this.recos.data = data)
      .catch(error => this.recos.loadError = { "text": error })
      .finally(() => this.recos.loading = false);
  }
});
</script>
