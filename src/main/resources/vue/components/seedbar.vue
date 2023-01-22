<template id="seedbar">
  <div class="nav-scroller bg-body shadow-sm" v-if="!seeds.loading">
    <nav class="nav" aria-label="Secondary navigation">
      <a class="nav-link" v-for="s in seeds.data" :href="`#${s.seedName.replaceAll(' ', '-')}`">{{ s.seedName }}</a>
    </nav>
  </div>
</template>
<script>
app.component("seedbar", {
  template: "#seedbar",
  props: ["seeds"]
});
</script>
<style>
.nav-scroller .nav {
  color: rgba(255, 255, 255, .75);
}

.nav-scroller .nav-link {
  padding-top: .75rem;
  padding-bottom: .75rem;
  font-size: .875rem;
  color: #6c757d;
}

.nav-scroller .nav-link:hover {
  color: #007bff;
}

.nav-scroller .active {
  font-weight: 500;
  color: #343a40;
}
</style>
