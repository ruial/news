<template id="searchresults">
  <ul class="list-group mb-3">
    <p>Total results: {{ totalResults }}</p>
    <li v-for="r in results" class="list-group-item d-flex justify-content-between align-items-center"> 
      <div class="d-flex flex-column">
        <a :href="`/news/${encodeURIComponent(r.news.id)}`">{{ r.news.title }}</a>
        <small><span>{{ r.relevance.toFixed(2) }}</span> relevance score</small>
        <small><span class="d-block">{{ r.news.score }} points - {{ r.news.commentsCount }} <a :href="r.news.commentsUrl" rel="nofollow">comments</a></span></small>
        <small>{{ formatDate(r.news.date) }}</small>
      </div>
    </li>
  </ul>
</template>
<script>
app.component("searchresults", {
  template: "#searchresults",
  props: ["results"],
  computed: {
    totalResults() {
      return this.results.length;
    }
  }
});
</script>
