<template id="search">
  <navbar active="search" :query="query"></navbar>
  <main class="container">
    <h3 class="mt-4 mb-3">Search</h3>
    <div class="mb-3">
      <searchform :query="query" :navbar="false"></searchform>
    </div>
    <p class="py-3" v-if="search.loading">Loading searches...</p>
    <p class="py-3" v-if="search.loadError">Failed to load search results! ({{ search.loadError.text }})</p>
    <p class="py-3" v-if="!search.loading && !search.loadError && query && search.data.length === 0">No search results to show.</p>
    <searchresults :results="search.data" v-if="!search.loading && search.data.length > 0"></searchresults>
  </main>
</template>
<script>
app.component("search", {
  template: "#search",
  data: function () {
    return {
      search: {
        loading: false,
        loadError: null,
        data: []
      },
      settings: loadSettings()
    }
  },
  created() {
    if (this.query) {
      this.loading = true;
      const { maxCountSearch, maxAgeDaysSearch } = this.settings;
      const extraParams = buildQueryParams(maxCountSearch, maxAgeDaysSearch);
      fetch(`/api/search/news?query=${this.query}&${extraParams}`).then(res => {
        if (res.ok) return res.json();
        return res.text().then(text => { throw new Error(text) });
      })
        .then(data => this.search.data = data)
        .catch(error => this.search.loadError = { "text": error })
        .finally(() => this.search.loading = false);
    }
  },
  computed: {
    query() {
      const urlParams = new URLSearchParams(window.location.search);
      return urlParams.get("query");
    }
  }
});
</script>
