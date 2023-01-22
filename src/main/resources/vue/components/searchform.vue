<template id="searchform">
  <form class="d-flex" role="search" action="/search" method="get">
    <input class="form-control me-2" type="search" placeholder="Search" name="query" :value="query">
    <button class="btn" :class="{
          'btn-success': !navbar,
          'btn-outline-success': navbar,
        }" type="submit">Search</button>
  </form>
</template>
<script>
app.component("searchform", {
  template: "#searchform",
  props: ["query", "navbar"]
});
</script>
